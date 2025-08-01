(ns passkeys.demo.ui.routes
  {:author "David Harrigan"}
  (:require
   [passkeys.demo.ui.dashboard.routes :as dashboard-ui]
   [passkeys.demo.ui.login.routes :as login-ui]
   [passkeys.demo.ui.middleware.authentication :refer [cookie-authentication-middleware]]))

(set! *warn-on-reflection* true)

(defn ^:private private-routes
  []
  ["" {:middleware cookie-authentication-middleware}
   (dashboard-ui/routes)])

(defn ^:private public-routes
  []
  (login-ui/routes))

(defn routes
  []
  [(public-routes) (private-routes)])
