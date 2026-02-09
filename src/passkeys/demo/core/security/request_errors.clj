(ns passkeys.demo.core.security.request-errors
  {:author "David Harrigan"})

(set! *warn-on-reflection* true)

(def missing-or-invalid-origin-header {:message "Missing or invalid Origin header" :data {:error :missing.or.invalid.origin.header}})
(def missing-or-invalid-sec-fetch-site-header {:message "Missing or invalid Sec-Fetch-Site header" :data {:error :missing.or.invalid.sec.fetch.site.header}})

(def unsafe-request {:message "Unsafe request" :data {:error :unsafe.request}})
