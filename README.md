# Cloudflare Ingress Guard

**Cloudflare Ingress Guard** is a small Kubernetes controller that protects your ingresses by automatically restricting
access to [Cloudflare’s official IP ranges](https://www.cloudflare.com/ips/).
It is designed to work with the **NGINX Ingress Controller**.

## Usage

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
