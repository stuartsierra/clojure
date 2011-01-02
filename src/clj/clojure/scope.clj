;;; Resource scopes for Clojure

;; by Stuart Sierra, http://stuartsierra.com/
;;
;; References:
;;
;; Resource Scopes (Clojure)
;; http://dev.clojure.org/display/design/Resource+Scopes
;;
;; Scope implementation in "streams" branch (Clojure)
;; https://github.com/clojure/clojure/blob/streams/src/clj/clojure/core.clj#L2005)
;;
;; Scope Guard Statement (D language)
;; http://www.digitalmars.com/d/2.0/statement.html#ScopeGuardStatement


(ns clojure.scope)

(defmacro declare-scope
  "Declares a Var representing a resource scope."
  [name]
  {:pre [(symbol? name)]}
  `(declare ~(vary-meta name assoc :dynamic true :scope true)))

(defn scope []
  "Returns a new resource scope."
  (ref {:handlers (list)
	:state :running
	:cause :success}))

(defn fail-scope
  "Marks scope s as having exited with a failure."
  [s]
  (dosync
   (alter s assoc :cause :failure)))

(defn exit-scope
  "Closes scope s and executes its handlers."
  [s]
  (dosync
   (alter s update-in [:state]
	  (fn [state]
	    (if (= state :running)
	      :done
	      (throw (IllegalArgumentException.
		      "exit-scope called a scope that has already exited"))))))
  (let [{:keys [handlers cause]} @s]
    (doseq [[condition f] handlers]
      (when (or (= :exit condition)
		(= cause condition))
	(f)))))

(defmacro with-scope
  "Executes body in a new scope bound to s, which must be a Var
  declared ^:dynamic. When body completes, normally or abnormally,
  executes callbacks declared with on-exit, on-failure, or
  on-success. Callbacks will be executed in the reverse order in which
  they were declared. If a callback function throws an exception,
  remaining callbacks will not be executed."
  [s & body]
  {:pre [(symbol? s)]}
  `(binding [~s (scope)]
     (try ~@body
	  (catch Throwable t#
	    (fail-scope ~s)
	    (throw t#))
	  (finally
	   (exit-scope ~s)))))

(defmacro add-scope-handler
  [s condition & body]
  {:pre [(symbol? s)
	 (#{:exit :success :failure} condition)]}
  `(if (and (bound? (var ~s)) (= :running (:state (deref ~s))))
     (dosync (alter ~s update-in [:handlers] conj
		    [~condition (fn [] ~@body)]))
     (throw (IllegalArgumentException.
	     ~(str "Tried to add on-" (name condition)
		   " handler to inactive scope " s)))))

(defmacro on-exit
  "Adds a callback to scope s which will execute body when the scope
  exits, either normally or because of an exception."
  [s & body]
  `(add-scope-handler ~s :exit ~@body))

(defmacro on-failure
  "Adds a callback to scope s which will execute body when the scope
  exits because of a thrown exception."
  [s & body]
  `(add-scope-handler ~s :failure ~@body))

(defmacro on-success
  "Adds a callback to scope s which will execute body when the scope
  exits normally."
  [s & body]
  `(add-scope-handler ~s :success ~@body))
