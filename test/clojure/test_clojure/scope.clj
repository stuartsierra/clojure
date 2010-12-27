(ns clojure.test-clojure.scope
  (:use clojure.scope
	clojure.test))

(declare-scope *s*)

(deftest t-scope-success
  (is (= "12345"
	 (with-out-str
	   (with-scope *s* 
	     (on-success *s* (pr 5)) 
	     (on-exit *s* (pr 4))
	     (pr 1)
	     (on-success *s* (pr 3))
	     (pr 2))))))

(deftest t-scope-failure
  (is (= "124"
	 (with-out-str
	   (try
	     (with-scope *s* 
	       (on-success *s* (pr 5)) 
	       (on-exit *s* (pr 4)) 
	       (pr 1)
	       (pr 2)
	       (throw (Exception. "Boom!"))
	       (pr 3)
	       (on-exit *s* (pr 6)))
	     (catch Exception e nil))))))

(deftest t-scope-agents
  (let [log-atom (atom #{})
	log (fn [message] (swap! log-atom conj message))]
    (let [a1 (agent nil)
	  a2 (agent nil)
	  a3 (agent nil)]
      (with-scope *s*
	(send a1 (fn [_] (on-exit *s* (log 5))))
	(send a2 (fn [_] (on-exit *s* (log 4))))
	(send a1 (fn [_] (on-exit *s* (log 3))))
	(send a2 (fn [_] (on-exit *s* (log 2))))
	(send a3 (fn [_] (on-exit *s* (log 1))))
	(await a1 a2 a3))
      (is (= #{1 2 3 4 5} @log-atom)))))