(ns clojure.bootstrap-compile)

;; Clojure's core libraries have to be compiled in this specific order.

(doseq [sym
	['clojure.core
	 'clojure.core.protocols
	 'clojure.main
	 'clojure.set
	 'clojure.xml
	 'clojure.zip
	 'clojure.inspector
	 'clojure.walk
	 'clojure.stacktrace
	 'clojure.template
	 'clojure.test
	 'clojure.test.tap
	 'clojure.test.junit
	 'clojure.pprint
	 'clojure.java.io
	 'clojure.repl
	 'clojure.java.browse
	 'clojure.java.javadoc
	 'clojure.java.shell
	 'clojure.java.browse-ui
	 'clojure.string
	 'clojure.data
	 'clojure.reflect]]
  (println "Compiling" sym)
  (compile sym))

;; We do not compile clojure.parallel because it depends on jsr166y.jar
