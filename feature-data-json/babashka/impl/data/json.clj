(ns babashka.impl.data.json
  {:no-doc true}
  (:require [clojure.data.json :as json]
            [sci.core :as sci :refer [copy-var]]))


(def jsn (sci/create-ns 'clojure.data.json nil))

(def data-json-namespace
  {'on-extra-throw (copy-var json/on-extra-throw jsn)
   'on-extra-throw-remaining (copy-var json/on-extra-throw-remaining jsn)
   'pprint (copy-var json/pprint jsn)
   'read (copy-var json/read jsn)
   'read-str (copy-var json/read-str jsn)
   'write (copy-var json/write jsn)
   'write-str (copy-var json/write-str jsn)})
