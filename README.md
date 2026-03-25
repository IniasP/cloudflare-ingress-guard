# Cloudflare Ingress Guard

**Cloudflare Ingress Guard** is a small Kubernetes controller that protects your ingresses by automatically restricting
access to [Cloudflare's official IP ranges](https://www.cloudflare.com/ips/).
The result is that it is no longer possible to bypass Cloudflare's "orange cloud" proxy service for DNS records pointing
to those ingresses.

It supports two modes, which can be used simultaneously:

- **NGINX Ingress Controller** — via `networking.k8s.io/v1 Ingress` resources
- **NGINX Gateway Fabric** — via the `CloudflareWhitelistFilter` CRD

Cloudflare IP ranges are fetched directly from Cloudflare (https://www.cloudflare.com/ips-v4
and https://www.cloudflare.com/ips-v6) and cached for 7 days.

---

## NGINX Ingress Controller

**IMPORTANT**: make sure your nginx ingress controller is configured with `service.externalTrafficPolicy: Local`. Otherwise, the filter will see an internal cluster IP instead of the connecting Cloudflare IP.

The controller watches all `Ingress` resources in the cluster.
If an ingress has the annotation:

  ```yaml
  cloudflare-ingress-guard.inias.eu/enabled: "true"
  ```

it ensures the annotation

  ```yaml
  nginx.ingress.kubernetes.io/whitelist-source-range: "<Cloudflare IPs>"
  ```

is set with the current Cloudflare IPv4 and IPv6 ranges.

### Caveats (Ingress)

If the `whitelist-source-range` annotation is already present, existing IP ranges it may contain are *not* preserved.

When the `cloudflare-ingress-guard.inias.eu/enabled` annotation is removed, the NGINX whitelist annotation will not be removed or cleaned up. Cleanup should be done manually; you may want to specify another type of protection or a different whitelist.
Alternatively, explicitly setting `cloudflare-ingress-guard.inias.eu/enabled: false` will drop any existing whitelist. This makes it easier to toggle the functionality for testing or debugging.

### Example (Ingress)

```yaml
apiVersion: networking.k8s.io/v1
kind: Ingress
metadata:
  name: my-app
  annotations:
    cloudflare-ingress-guard.inias.eu/enabled: "true"
spec:
  rules:
    - host: my-app.example.com
      http:
        paths:
          - path: /
            pathType: Prefix
            backend:
              service:
                name: my-app
                port:
                  number: 80
```

### RBAC requirements (Ingress)

The controller needs RBAC permissions to modify the relevant ingresses. The following rule suffices:

```yaml
- apiGroups: [networking.k8s.io]
  resources: [ingresses]
  verbs: [get, list, watch, update, patch]
```

---

## NGINX Gateway Fabric

The controller provides a `CloudflareWhitelistFilter` CRD for use with NGINX Gateway Fabric.
When you create a `CloudflareWhitelistFilter` in a namespace, the operator creates and maintains a
[`SnippetsFilter`](https://docs.nginx.com/nginx-gateway-fabric/reference/api/#gateway.nginx.org/v1alpha1.SnippetsFilter)
(`gateway.nginx.org/v1alpha1`) in the same namespace, populated with NGINX `allow` directives for all
current Cloudflare IP ranges followed by `deny all`.

You then reference the produced `SnippetsFilter` from your `HTTPRoute` rules via an `ExtensionRef` filter.

> **Note:** The Kubernetes Gateway API has no standard, implementation-agnostic mechanism for IP allowlisting.
> This feature therefore requires **NGINX Gateway Fabric v1.4+** with the `SnippetsFilter` feature gate enabled.

**IMPORTANT**: The NGF `Gateway` service must be configured with `externalTrafficPolicy: Local`. Otherwise, the filter will see an internal cluster IP instead of the connecting Cloudflare IP.

### How it works

1. Create a `CloudflareWhitelistFilter` in each namespace where you have `HTTPRoute` resources to protect.
2. The operator creates a `SnippetsFilter` with the same name (or the name specified in `spec.snippetsFilterName`) in the same namespace.
3. Reference the `SnippetsFilter` from your `HTTPRoute` rules via an `ExtensionRef` filter.

### Example (Gateway)

```yaml
# One CloudflareWhitelistFilter per namespace:
apiVersion: cloudflareingressguard.inias.eu/v1alpha1
kind: CloudflareWhitelistFilter
metadata:
  name: cloudflare-ips
  namespace: my-app
# spec.snippetsFilterName is optional; defaults to metadata.name
```

```yaml
# Reference the produced SnippetsFilter from your HTTPRoute:
apiVersion: gateway.networking.k8s.io/v1
kind: HTTPRoute
metadata:
  name: my-app
  namespace: my-app
spec:
  parentRefs:
    - name: my-gateway
  rules:
    - matches:
        - path:
            type: PathPrefix
            value: /
      filters:
        - type: ExtensionRef
          extensionRef:
            group: gateway.nginx.org
            kind: SnippetsFilter
            name: cloudflare-ips   # same as CloudflareWhitelistFilter name
      backendRefs:
        - name: my-app
          port: 80
```

After reconciliation, the operator creates a `SnippetsFilter` like:

```yaml
apiVersion: gateway.nginx.org/v1alpha1
kind: SnippetsFilter
metadata:
  name: cloudflare-ips
  namespace: my-app
  ownerReferences:
    - apiVersion: cloudflareingressguard.inias.eu/v1alpha1
      kind: CloudflareWhitelistFilter
      name: cloudflare-ips
      controller: true
      blockOwnerDeletion: true
spec:
  snippets:
    - context: http.server.location
      value: |
        allow 173.245.48.0/20;
        allow 103.21.244.0/22;
        # ... all Cloudflare CIDRs ...
        deny all;
```

### Optional: custom SnippetsFilter name

If you want the produced `SnippetsFilter` to have a different name than the `CloudflareWhitelistFilter`,
use `spec.snippetsFilterName`:

```yaml
apiVersion: cloudflareingressguard.inias.eu/v1alpha1
kind: CloudflareWhitelistFilter
metadata:
  name: my-filter
  namespace: my-app
spec:
  snippetsFilterName: cloudflare-allowlist
```

This creates a `SnippetsFilter` named `cloudflare-allowlist` instead of `my-filter`.

### Enabling the SnippetsFilter feature gate in NGF

The `SnippetsFilter` feature must be explicitly enabled in your NGF installation. Add the following
to your NGF Helm values:

```yaml
nginx-gateway:
  config:
    featureGates:
      SnippetsFilters: true
```

Or if installing via manifests, set the `--feature-gates=SnippetsFilters=true` flag on the
`nginx-gateway` container.

### RBAC requirements (Gateway)

The controller needs the following RBAC permissions for Gateway API mode:

```yaml
- apiGroups: ["cloudflareingressguard.inias.eu"]
  resources: ["cloudflarewhitelistfilters"]
  verbs: ["get", "list", "watch", "update", "patch"]
- apiGroups: ["cloudflareingressguard.inias.eu"]
  resources: ["cloudflarewhitelistfilters/status"]
  verbs: ["get", "update", "patch"]
- apiGroups: ["gateway.nginx.org"]
  resources: ["snippetsfilters"]
  verbs: ["get", "list", "watch", "create", "update", "patch", "delete"]
```

### Caveats (Gateway)

Since `SnippetsFilter` is namespace-scoped and `HTTPRoute` `extensionRef` filters are local references
(no cross-namespace support), you need one `CloudflareWhitelistFilter` per namespace where you have
`HTTPRoute` resources to protect.

When a `CloudflareWhitelistFilter` is deleted, the `SnippetsFilter` is garbage-collected automatically.
However, any `HTTPRoute` rules that reference the deleted `SnippetsFilter` will report a `ResolvedRefs`
error from NGF until the `ExtensionRef` filter is removed from the `HTTPRoute` manifest.
