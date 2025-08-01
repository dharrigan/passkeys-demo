(ns passkeys.demo.ui.utils.htmx
  {:author "David Harrigan"}
  (:require
   [ring.util.response :as response]))

(set! *warn-on-reflection* true)

(defn hx-redirect
  ([location] (hx-redirect (response/response nil) location))
  ([response location]
   (response/header response "HX-Redirect" location)))
