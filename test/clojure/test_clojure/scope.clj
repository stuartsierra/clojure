(ns clojure.test-clojure.scope
  (:use clojure.scope
	clojure.test)
  (:import (java.util.concurrent CountDownLatch)))

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

(deftest t-scope-transactions
  (let [log-atom (atom #{})
	log (fn [message] (swap! log-atom conj message))]
    (let [r1 (ref nil)
	  r2 (ref nil)
	  r3 (ref nil)]
      (with-scope *s*
	(dosync
	 (ref-set r1 nil)
	 (on-exit *s* (log 5))
	 (ref-set r2 nil)
	 (on-exit *s* (log 4)))
	(try (dosync
	      (on-exit *s* (log 3))
	      (ref-set r3 nil)
	      (on-exit *s* (log 2))
	      (throw (Exception. "Boom!"))
	      (on-exit *s* (log 1)))
	     (catch Exception e nil)))
      (is (= #{4 5} @log-atom)))))

(deftest t-scope-futures
  (let [log-atom (atom [])
	log (fn [message] (swap! log-atom conj message))
	latch1 (CountDownLatch. 1)
	latch2 (CountDownLatch. 1)]
    (with-scope *s*
      (future (.await latch1)
	      (on-exit *s* (log :b))
	      (.countDown latch2))
      (on-exit *s* (log :a))
      (.countDown latch1)
      (.await latch2))
    (is (= @log-atom [:b :a]))))

;; Test that an attempt to add handlers to scope throws an exception
;; if that scope has already ended.  The sequence of events in this
;; test is described by this table:
;;
;;     | Main thread     | Future's thread |
;;     |-----------------+-----------------|
;;     |                 | Await latch 1   |
;;     | Enter scope *s* |                 |
;;     | Add handler :a  |                 |
;;     | Close latch 1   |                 |
;;     | Await latch 2   | Add handler :b  |
;;     |                 | Close latch 2   |
;;     | Exit scope *s*  | Await latch 3   |
;;     | Close latch 3   |                 |
;;     | Await latch 4   | Add handler :c  |
;;     |                 | Catch exception |
;;     |                 | Close latch 4   |
;;     | Assert result   |                 |
(deftest t-scope-error-if-not-running
  (let [log-atom (atom [])
	log (fn [message] (swap! log-atom conj message))
	latch1 (CountDownLatch. 1)
	latch2 (CountDownLatch. 1)
	latch3 (CountDownLatch. 1)
	latch4 (CountDownLatch. 1)]
    (with-scope *s*
      (future (try (.await latch1)
		   (on-exit *s* (log :b))
		   (.countDown latch2)
		   (.await latch3)
		   (on-exit *s* (log :c))
		(catch Exception e
		  (log :error))
		(finally (.countDown latch4))))
      (on-exit *s* (log :a))
      (.countDown latch1)
      (.await latch2))
    (.countDown latch3)
    (.await latch4)
    (is (= @log-atom [:error :b :a]))))
