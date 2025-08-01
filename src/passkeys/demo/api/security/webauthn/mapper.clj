(ns passkeys.demo.api.security.webauthn.mapper
  {:author "David Harrigan"}
  (:require
   [camel-snake-kebab.core :as csk]
   [camel-snake-kebab.extras :as csk-extras]
   [cheshire.core :as json])
  (:import
   [com.yubico.webauthn.data PublicKeyCredential]))

(set! *warn-on-reflection* true)

(defn ^:private jsonify-body
  [body]
  (-> (csk-extras/transform-keys csk/->camelCase body)
      (update :response #(-> % (assoc :clientDataJSON (get % :clientDataJson)) (dissoc :clientDataJson)))
      (json/generate-string)))

(defn request->registration-response
  [request]
  (let [{{{:keys [registration-id]} :path} :parameters :keys [body-params]} request
        client-response (PublicKeyCredential/parseRegistrationResponseJson (jsonify-body body-params))]
    {:registration-id registration-id :client-response client-response}))

(defn request->authentication-response
  [request]
  (let [{{{:keys [authentication-id]} :path} :parameters :keys [body-params]} request
        client-response (PublicKeyCredential/parseAssertionResponseJson (jsonify-body body-params))]
    {:authentication-id authentication-id :client-response client-response}))
