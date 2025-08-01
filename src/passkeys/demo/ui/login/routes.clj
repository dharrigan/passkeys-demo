(ns passkeys.demo.ui.login.routes
  {:author "David Harrigan"}
  (:require
   [passkeys.demo.system.constants :refer [content-type html]]
   [passkeys.demo.ui.login.handler :as handler]
   [passkeys.demo.ui.login.spec :as spec]
   [passkeys.demo.ui.middleware.authentication :refer [basic-authentication-middleware cookie-authentication-middleware magic-link-authentication-middleware]]
   [ring.middleware.anti-forgery :as anti-forgery]))

(set! *warn-on-reflection* true)

(def ^:private custom-anti-forgery-response {:status 403 :headers {content-type html} :body "Invalid anti-forgery token"})

(defn ^:private public-routes
  []
  [["/" {:get {:middleware cookie-authentication-middleware
               :handler handler/dashboard}}] ;; if the private-authentication-middleware succeeds, there is a existing valid session, thus take the user to the dashboard.
   ["/login" {:middleware [[anti-forgery/wrap-anti-forgery {:error-response custom-anti-forgery-response}]]
              :get {:handler handler/login
                    :parameters {:query spec/login-request-with-reason}}
              :post {:middleware basic-authentication-middleware
                     :parameters {:form spec/basic-login-request}
                     :handler handler/basic-login-success}}] ;; it's success, as the middleware will have already validated the authentication.
   ["/login/magic-link" {:get {:middleware magic-link-authentication-middleware
                               :parameters {:query spec/magic-link-login-request}
                               :handler handler/magic-link-login-success}}] ;; it's success, as the middleware will have already validated the authentication.
   ["/logout" {:post handler/logout}]])

(defn routes
  []
  (public-routes))
