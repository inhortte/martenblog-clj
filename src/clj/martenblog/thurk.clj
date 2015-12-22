(ns martenblog.thurk
  (:refer-clojure :exclude [find])
  (:require [monger.core :as mg]
            [monger.collection :as mc]
            [clojure.string :refer [trim]]
            [clojure.set :refer [union]]))

(def ^:private db-name "martenblog")

(defn entry-count []
  (let [conn (mg/connect)
        db (mg/get-db conn db-name)]
    (mc/count db "entry")))
