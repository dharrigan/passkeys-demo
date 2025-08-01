(ns passkeys.demo.ui.login.render
  {:author "David Harrigan"}
  (:require
   [passkeys.demo.core.session.impl :as session]
   [passkeys.demo.ui.middleware.exceptions :as exceptions]
   [passkeys.demo.ui.utils.cookie :as cookie-utils]
   [passkeys.demo.ui.utils.htmx :as htmx-utils]
   [passkeys.demo.ui.utils.thymeleaf :as thymeleaf]
   [ring.util.response :as response]))

(set! *warn-on-reflection* true)

(defn login
  [{:keys [anti-forgery-token] :as request} reason]
  (let [data (cond-> {:csrf anti-forgery-token}
               reason (assoc :error (exceptions/handle-error request {:error (keyword reason) :type :ui.session})))]
    (thymeleaf/html "/login/index" request data)))

(defn basic-login-success
  [{:keys [magic-link-token] :as request}]
  (thymeleaf/html "/login/magic-links/notification" request {:magic-link-token magic-link-token}))

(defn magic-link-login-success
  [{{:keys [session-id]} :identity :keys [system] :as request}]
  (-> (response/redirect "/dashboard")
      (cookie-utils/set-session-cookie session-id system)))

(defn logout
  [{{:keys [session-id]} :identity :keys [system] :as request} reason]
  (session/delete-session session-id system)
  (-> (response/response nil)
      (cookie-utils/unset-session-cookie system)
      (htmx-utils/hx-redirect (if reason (str "/login?reason=" reason) "/login"))))
