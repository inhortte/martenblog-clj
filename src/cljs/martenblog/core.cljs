(ns martenblog.core
    (:require [secretary.core :as secretary :include-macros true]
              [accountant.core :as accountant]
              [reagent.core :as reagent]
              [reagent.session :as session]
              [martenblog.buttock :refer [buttock-page]]))

;; -------------------------
;; Views

(defn home-page []
  [:div [:h2 "Welcome to martenblog"]
   [:div [:a {:href "/about"} "go to about page"]]])

(defn about-page []
  [:div [:h2 "About martenblog"]
   [:div [:a {:href "/buttock"} "go to the mustelid page"]]])

(defn current-page []
  [:div [(session/get :current-page)]])

;; -------------------------
;; Routes

(secretary/defroute "/" []
  (session/put! :current-page #'home-page))

(secretary/defroute "/about" []
  (session/put! :current-page #'about-page))

(secretary/defroute "/buttock" []
  (session/put! :current-page #'buttock-page))

;; -------------------------
;; Initialize app

(defn mount-root []
  (reagent/render [current-page] (.getElementById js/document "app")))

(defn init! []
  (accountant/configure-navigation!)
  (accountant/dispatch-current!)
  (mount-root))
