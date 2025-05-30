(ns babashka.impl.classpath
  {:no-doc true}
  (:refer-clojure :exclude [add-classpath])
  (:require [babashka.impl.clojure.main :refer [demunge]]
            [clojure.java.io :as io]
            [clojure.string :as str]
            [sci.core :as sci])
  (:import [java.util.jar Manifest]
           (java.net URL)))

(set! *warn-on-reflection* true)

(defn getResource [^babashka.impl.URLClassLoader class-loader resource-paths url?]
  (some (fn [resource]
          (when-let [^java.net.URL res (.findResource class-loader resource)]
            (if url?
              res
              {:file (if (= "jar" (.getProtocol res))
                       resource
                       (.getFile res))
               :source (slurp res)})))
        resource-paths))

(def path-sep (System/getProperty "path.separator"))

(defn ->url ^java.net.URL [^String s]
  (.toURL (java.io.File. s)))

(defn new-loader ^babashka.impl.URLClassLoader
  ([paths]
   (babashka.impl.URLClassLoader. (into-array java.net.URL (map ->url paths)))))

(def ^babashka.impl.URLClassLoader the-url-loader (delay (new-loader [])))

(defn add-classpath
  "Adds extra-classpath, a string as for example returned by clojure
  -Spath, to the current classpath."
  [^String extra-classpath]
  (let [paths (.split extra-classpath path-sep)
        paths (map ->url paths)
        loader @the-url-loader]
    (run! (fn [path]
            (._addURL ^babashka.impl.URLClassLoader loader path)
            loader)
          paths)
    ;; (run! prn (.getURLs the-url-loader))
    (System/setProperty "java.class.path"
                        (let [system-cp (System/getProperty "java.class.path")]
                          (-> (cond-> system-cp
                                (not (str/blank? system-cp)) (str path-sep))
                              (str extra-classpath)))))
  nil)

(defn resource-paths [namespace]
  (let [ns-str (name namespace)
        ^String ns-str (namespace-munge ns-str)
        ;; do NOT pick the platform specific file separator here, since that doesn't work for searching in .jar files
        ;; (io/file "foo" "bar/baz") does work on Windows, despite the forward slash
        base-path (.replace ns-str "." "/")
        resource-paths (mapv #(str base-path %) [".bb" ".clj" ".cljc"])]
    resource-paths))

(defn source-for-namespace [loader namespace opts]
  (let [rps (resource-paths namespace)]
    (getResource loader rps opts)))

(defn main-ns [manifest-resource]
  (with-open [is (io/input-stream manifest-resource)]
    (some-> (Manifest. is)
            (.getMainAttributes)
            (.getValue "Main-Class")
            (demunge))))

(defn split-classpath
  "Returns the classpath as a seq of strings, split by the platform
  specific path separator."
  ([^String cp] (vec (when cp (.split cp path-sep)))))

(defn get-classpath
  "Returns the current classpath as set by --classpath, BABASHKA_CLASSPATH and add-classpath."
  []
  (let [cp (System/getProperty "java.class.path")]
    (when-not (str/blank? cp)
      cp)))

(defn resource
  (^URL [path] (resource path @the-url-loader))
  (^URL [path loader]
   (if (str/starts-with? path "/") nil ;; non-relative paths always return nil
       (getResource loader [path] true))))

(def cns (sci/create-ns 'babashka.classpath nil))

(def classpath-namespace
  {:obj cns
   'add-classpath (sci/copy-var add-classpath cns)
   'split-classpath (sci/copy-var split-classpath cns)
   'get-classpath (sci/copy-var get-classpath cns)})

;;;; Scratch

(comment
  (def cp "src:feature-xml:feature-core-async:feature-yaml:feature-csv:feature-transit:feature-java-time:feature-java-nio:sci/src:babashka.curl/src:babashka.pods/src:resources:sci/resources:/Users/borkdude/.m2/repository/com/cognitect/transit-java/1.0.343/transit-java-1.0.343.jar:/Users/borkdude/.m2/repository/org/clojure/clojure/1.10.2-alpha1/clojure-1.10.2-alpha1.jar:/Users/borkdude/.m2/repository/commons-codec/commons-codec/1.10/commons-codec-1.10.jar:/Users/borkdude/.m2/repository/org/clojure/tools.analyzer/1.0.0/tools.analyzer-1.0.0.jar:/Users/borkdude/.m2/repository/org/clojure/tools.logging/0.6.0/tools.logging-0.6.0.jar:/Users/borkdude/.m2/repository/org/clojure/core.specs.alpha/0.2.44/core.specs.alpha-0.2.44.jar:/Users/borkdude/.m2/repository/org/clojure/spec.alpha/0.2.187/spec.alpha-0.2.187.jar:/Users/borkdude/.m2/repository/org/clojure/tools.cli/1.0.194/tools.cli-1.0.194.jar:/Users/borkdude/.m2/repository/org/clojure/tools.analyzer.jvm/1.0.0/tools.analyzer.jvm-1.0.0.jar:/Users/borkdude/.m2/repository/borkdude/graal.locking/0.0.2/graal.locking-0.0.2.jar:/Users/borkdude/.m2/repository/com/fasterxml/jackson/dataformat/jackson-dataformat-cbor/2.10.2/jackson-dataformat-cbor-2.10.2.jar:/Users/borkdude/.m2/repository/com/googlecode/json-simple/json-simple/1.1.1/json-simple-1.1.1.jar:/Users/borkdude/.m2/repository/org/flatland/ordered/1.5.9/ordered-1.5.9.jar:/Users/borkdude/.m2/repository/org/postgresql/postgresql/42.2.12/postgresql-42.2.12.jar:/Users/borkdude/.m2/repository/fipp/fipp/0.6.22/fipp-0.6.22.jar:/Users/borkdude/.m2/repository/com/fasterxml/jackson/core/jackson-core/2.10.2/jackson-core-2.10.2.jar:/Users/borkdude/.m2/repository/org/yaml/snakeyaml/1.25/snakeyaml-1.25.jar:/Users/borkdude/.m2/repository/org/ow2/asm/asm/5.2/asm-5.2.jar:/Users/borkdude/.gitlibs/libs/clj-commons/conch/9aa7724e925cb8bf163e0b62486dd420b84e5f0b/src:/Users/borkdude/.m2/repository/org/javassist/javassist/3.18.1-GA/javassist-3.18.1-GA.jar:/Users/borkdude/.m2/repository/seancorfield/next.jdbc/1.0.424/next.jdbc-1.0.424.jar:/Users/borkdude/.m2/repository/org/clojure/data.xml/0.2.0-alpha6/data.xml-0.2.0-alpha6.jar:/Users/borkdude/.m2/repository/org/msgpack/msgpack/0.6.12/msgpack-0.6.12.jar:/Users/borkdude/.m2/repository/borkdude/edamame/0.0.11-alpha.9/edamame-0.0.11-alpha.9.jar:/Users/borkdude/.m2/repository/org/clojure/data.csv/1.0.0/data.csv-1.0.0.jar:/Users/borkdude/.m2/repository/com/cognitect/transit-clj/1.0.324/transit-clj-1.0.324.jar:/Users/borkdude/.m2/repository/clj-commons/clj-yaml/0.7.1/clj-yaml-0.7.1.jar:/Users/borkdude/.m2/repository/org/clojure/core.rrb-vector/0.1.1/core.rrb-vector-0.1.1.jar:/Users/borkdude/.m2/repository/persistent-sorted-set/persistent-sorted-set/0.1.2/persistent-sorted-set-0.1.2.jar:/Users/borkdude/.m2/repository/cheshire/cheshire/5.10.0/cheshire-5.10.0.jar:/Users/borkdude/.m2/repository/tigris/tigris/0.1.2/tigris-0.1.2.jar:/Users/borkdude/.m2/repository/org/clojure/tools.reader/1.3.2/tools.reader-1.3.2.jar:/Users/borkdude/.m2/repository/datascript/datascript/0.18.11/datascript-0.18.11.jar:/Users/borkdude/.m2/repository/org/hsqldb/hsqldb/2.4.0/hsqldb-2.4.0.jar:/Users/borkdude/.m2/repository/org/clojure/core.memoize/0.8.2/core.memoize-0.8.2.jar:/Users/borkdude/.m2/repository/org/clojure/data.priority-map/0.0.7/data.priority-map-0.0.7.jar:/Users/borkdude/.m2/repository/org/clojure/java.data/1.0.64/java.data-1.0.64.jar:/Users/borkdude/.m2/repository/borkdude/sci.impl.reflector/0.0.1/sci.impl.reflector-0.0.1.jar:/Users/borkdude/.m2/repository/nrepl/bencode/1.1.0/bencode-1.1.0.jar:/Users/borkdude/.m2/repository/org/clojure/core.cache/0.8.2/core.cache-0.8.2.jar:/Users/borkdude/.m2/repository/org/clojure/core.async/1.1.587/core.async-1.1.587.jar:/Users/borkdude/.m2/repository/com/fasterxml/jackson/dataformat/jackson-dataformat-smile/2.10.2/jackson-dataformat-smile-2.10.2.jar:/Users/borkdude/.m2/repository/org/clojure/data.codec/0.1.0/data.codec-0.1.0.jar:/Users/borkdude/.m2/repository/javax/xml/bind/jaxb-api/2.3.0/jaxb-api-2.3.0.jar")
  (def l (loader cp))
  (source-for-namespace l 'babashka.impl.cheshire nil)
  (time (:file (source-for-namespace l 'cheshire.core nil)))) ;; 20ms -> 2.25ms
