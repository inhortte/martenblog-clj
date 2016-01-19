(ns martenblog.thurk
  (:require [monger.core :as mg]
            [monger.collection :as mc]
            [monger.query :as mq]
            [monger.operators :as mo]
            [clojure.string :refer [trim split]]
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
         (map #(hash-map :id (get % "_id")
                         :topic (get % "topic")
                         :entry-count (count (get % "entry_ids"))))
         (sort-by :entry-count >))))

(defn entries [topics page-number]
  (let [db (mg/get-db @conn db-name)
        topic-query (if (or (nil? topics) (empty? (trim topics)))
                      () (map #(hash-map :topic_ids (java.lang.Integer. %)) (split topics #",")))]
    (mq/with-collection db "entry"
      (mq/find (if (empty? topic-query) {} {mo/$or topic-query}))
      (mq/sort {:created_at -1})
      (mq/skip (* 11 (dec (java.lang.Integer. page-number))))
      (mq/limit 11))))
