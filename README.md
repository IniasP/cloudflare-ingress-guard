# Cloudflare Ingress Guard

**Cloudflare Ingress Guard** is a small Kubernetes controller that protects your ingresses by automatically restricting
access to [Cloudflare’s official IP ranges](https://www.cloudflare.com/ips/).
The result is that it is no longer possible to bypass Cloudflare's "orange cloud" proxy service for DNS records pointing
to those ingresses.
It is designed to work with the **NGINX Ingress Controller**.

## Usage

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

Cloudflare IP ranges are fetched directly from Cloudflare (https://www.cloudflare.com/ips-v4
and https://www.cloudflare.com/ips-v6) and cached for 7 days.

## Caveats

If the `whitelist-source-range` annotation is already present, existing IP ranges it may contain are *not* preserved.

When the `cloudflare-ingress-guard.inias.eu/enabled` annotation is removed, the NGINX whitelist annotation will not be removed or cleaned up. Cleanup should be done manually; you may want to specify another type of protection or a different whitelist.
Alternatively, explicitly setting `cloudflare-ingress-guard.inias.eu/enabled: false` will drop any existing whitelist. This makes it easier to toggle the functionality for testing or debugging.

## Example

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
