(ns passkeys.demo.api.security.webauthn.handler
  {:author "David Harrigan"}
  (:require
   [passkeys.demo.api.security.webauthn.mapper :as mapper]
   [passkeys.demo.api.security.webauthn.passkeys :as passkeys]
   [passkeys.demo.system.constants :refer [x-authentication-id x-registration-id]]
   [passkeys.demo.ui.utils.cookie :as cookie-utils]
   [ring.util.response :as response]))

(set! *warn-on-reflection* true)

(defn registration-start
  [{{:keys [account]} :identity :keys [system] :as request}]
  (let [{:keys [registration-id registration-request-options]} (passkeys/registration-start account system)]
    (-> (response/response registration-request-options)
        (response/header x-registration-id registration-id))))

(defn registration-verify
  [{{:keys [account]} :identity :keys [system] :as request}]
  (let [registration-response (mapper/request->registration-response request)]
    (passkeys/registration-verify account registration-response system)
    (response/response {:verified true})))

(defn authentication-begin
  [{:keys [system] :as request}]
  (let [{:keys [authentication-id authentication-request-options]} (passkeys/authentication-start system)]
    (-> (response/response authentication-request-options)
        (response/header x-authentication-id authentication-id))))

(defn authentication-verify
  [{:keys [system] :as request}]
  (let [authentication-response (mapper/request->authentication-response request)
        {:keys [session-id]} (passkeys/authentication-verify authentication-response system)]
    (-> (response/response {:verified true :redirect "/dashboard"})
        (cookie-utils/set-session-cookie session-id system))))
