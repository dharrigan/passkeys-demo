(ns passkeys.demo.ui.dashboard.routes
  {:author "David Harrigan"}
  (:require
   [passkeys.demo.ui.dashboard.handler :as handler]))

(set! *warn-on-reflection* true)

(defn ^:private private-routes
  []
  ["/dashboard"
   ["" {:get {:handler handler/index}}]])

(defn routes
  []
  (private-routes))
