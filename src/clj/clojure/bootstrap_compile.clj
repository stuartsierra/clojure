;   Copyright (c) Rich Hickey. All rights reserved.
;   The use and distribution terms for this software are covered by the
;   Eclipse Public License 1.0 (http://opensource.org/licenses/eclipse-1.0.php)
;   which can be found in the file epl-v10.html at the root of this distribution.
;   By using this software in any fashion, you are agreeing to be bound by
;   the terms of this license.
;   You must not remove this notice, or any other, from this software.

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
