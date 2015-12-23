(ns martenblog.thurk
  (:require [monger.core :as mg]
            [monger.collection :as mc]
            [clojure.string :refer [trim]]
            [clojure.set :refer [union]]))

(def ^:private db-name "martenblog")

(def conn (atom (mg/connect)))

(defn entry-count []
  (let [db (mg/get-db @conn db-name)]
    (mc/count db "entry")))

(defn topics
  "[{:topic \"lather\" :entry-count 666}, ...] each sorted desc by entry-count\n
  Usage: (topics)"
  []
  (let [db (mg/get-db @conn db-name)]
    (->> (mc/find (mg/get-db @conn db-name) "topic" {} {:topic 1 :entry_ids 1})
         (map #(hash-map :topic (get % "topic") :entry-count (count (get % "entry_ids"))))
         (sort-by :entry-count >))))
