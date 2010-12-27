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
