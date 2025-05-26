(ns babashka.impl.clj.bom
  {:no-doc true}
  (:require [clj-bom.core :as bom]
            [sci.core :as sci :refer [copy-var]]))


(def bomns (sci/create-ns 'clj-bom.core nil))

(def clj-bom-namespace
  {'bom-reader (copy-var bom/bom-reader bomns)
   'bom-writer (copy-var bom/bom-writer bomns)})
