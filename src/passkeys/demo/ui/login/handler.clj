(ns passkeys.demo.ui.login.handler
  {:author "David Harrigan"}
  (:require
   [passkeys.demo.ui.login.mapper :as mapper]
   [passkeys.demo.ui.login.render :as render]
   [ring.util.response :as response]))

(set! *warn-on-reflection* true)

(defn dashboard
  [_request]
  (response/redirect "/dashboard"))

(defn login
  [request]
  (let [reason (mapper/request->login request)]
    (render/login request reason)))

(defn basic-login-success
  [request]
  (render/basic-login-success request))

(defn magic-link-login-success
  [request]
  (render/magic-link-login-success request))

(defn logout
  ([request] (logout request nil))
  ([request error]
   (render/logout request error)))
