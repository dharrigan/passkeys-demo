(ns passkeys.demo.api.security.webauthn.routes
  {:author "David Harrigan"}
  (:require
   [passkeys.demo.api.security.webauthn.handler :as handler]
   [passkeys.demo.api.security.webauthn.spec :as spec]
   [passkeys.demo.ui.middleware.authentication :refer [cookie-authentication-middleware]]))

(set! *warn-on-reflection* true)

(defn private-routes
  []
  ["/registration" {:middleware cookie-authentication-middleware}
   ["/start" {:get {:handler handler/registration-start}}]
   ["/verify/:registration-id" {:post {:handler handler/registration-verify
                                       :parameters {:path spec/registration-verify-registration-id}}}]])

(defn public-routes
  []
  ["/authentication"
   ["/start" {:get {:handler handler/authentication-begin}}]
   ["/verify/:authentication-id" {:post {:handler handler/authentication-verify
                                         :parameters {:path spec/authentication-verify-authentication-id}}}]])

(defn routes
  []
  ["/webauthn" [(public-routes) (private-routes)]])
