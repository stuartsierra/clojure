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

(defn handle-scope-exit [s cause]
  (await s)
  (doseq [[condition f] @s]
    (when (or (= :exit condition)
	      (= cause condition))
      (f))))

(defmacro with-scope
  "Executes body in scope s, which must have been previously declared
  with declare-scope. When body completes, normally or abnormally,
  executes callbacks declared with on-exit, on-failure, or
  on-success. Callbacks will be executed in the reverse order in which
  they were declared. If a callback function throws an exception,
  remaining callbacks will not be executed."
  [s & body]
  {:pre [(symbol? s)]}
  `(binding [~s (agent (list))]
     (try (let [result# (do ~@body)]
	    (handle-scope-exit ~s :success)
	    result#)
	  (catch Throwable t#
	    (handle-scope-exit ~s :failure)
	    (throw t#)))))

(defmacro on-exit
  "Adds a callback to scope s which will execute body when the scope
  exits, either normally or because of an exception."
  [s & body]
  {:pre [(symbol? s)]}
  `(send ~s conj [:exit (fn [] ~@body)]))

(defmacro on-failure
  "Adds a callback to scope s which will execute body when the scope
  exits because of a thrown exception."
  [s & body]
  {:pre [(symbol? s)]}
  `(send ~s conj [:failure (fn [] ~@body)]))

(defmacro on-success
  "Adds a callback to scope s which will execute body when the scope
  exits normally."
  [s & body]
  {:pre [(symbol? s)]}
  `(send ~s conj [:success (fn [] ~@body)]))
