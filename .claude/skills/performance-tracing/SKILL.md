---
name: performance-tracing
description: Use this skill when a mobile team asks about naming conventions for OpenTelemetry spans, trace attributes, or business logic instrumentation. Triggers include questions about span naming, OTel on mobile, performance tracing (not TTI/TTID), diagnostic attributes, or how to structure telemetry for feature teams. Use this even if the user only mentions "tracing", "spans", "observability", or "instrumentation" in a mobile context.
---

# Mobile Telemetry Naming Convention

A skill for helping mobile teams define consistent, diagnostic OpenTelemetry span names and attributes — focused on business logic, not UI/rendering metrics.

---

## Core Naming Structure

```
<team>.<domain>.<operation>

checkout.payment.authorize
discovery.search.query.execute
cart.item.update
```

- **team** — owning team, maps to on-call rotation
- **domain** — product area within that team
- **operation** — verb + noun describing what happened

Use 3 levels for most spans. Add a 4th only when a domain has meaningfully different sub-operations (e.g. `checkout.payment.card.tokenize` vs `checkout.payment.wallet.authorize`).

---

## When to Create a Span

A span is worth creating if a developer could ask "why is this slow or failing?" and the answer would matter to the business. If a user wouldn't notice it taking longer, it probably doesn't need a top-level span.

Good candidates:
- Multi-step workflows (checkout, onboarding)
- Operations with variable outcomes (cache hit vs miss, stock available vs not)
- Background work that affects the next user interaction

Not worth a span:
- Pure UI rendering (use TTI/TTID metrics instead)
- Internal utility functions with no business meaning
- Operations that always take <5ms

---

## Attribute Tiers

### Tier 1 — Required on every span (platform enforces)
- `team` — owning team name
- `outcome` — `success` | `failed` | `partial`
- `user.is_guest` — boolean

### Tier 2 — Required per domain (team lead enforces)
The business-specific attributes defined per span below. Applied consistently across all spans in that domain.

### Tier 3 — Added during debugging
Attributes added after an incident to answer a question you couldn't answer. Keep a running list per team.

---

## Ecommerce Reference Span List

```
# Checkout
checkout.order.submit
checkout.payment.authorize
checkout.cart.validate

# Discovery
discovery.search.query.execute
discovery.pdp.load

# Cart
cart.update                        # use cart.action attribute to differentiate add/remove/quantity

# Fulfillment
fulfillment.inventory.check
fulfillment.tracking.status.fetch

# Account
account.auth.token.refresh
```

---

## Ecommerce Reference Attributes

**Every span**
- `team` — owning team name
- `outcome` — `success` | `failed` | `partial`
- `user.is_guest` — boolean

**checkout.order.submit**
- `cart.item_count`
- `cart.value_cents` — always integers, never floats
- `cart.promo_count`
- `checkout.attempt_count`

**checkout.payment.authorize**
- `payment.method` — `card` | `apple_pay` | `google_pay` | `paypal` | `klarna`
- `payment.provider` — `stripe` | `adyen` | `braintree`
- `payment.retry_count`
- `payment.decline_code` — only on failure

**cart.update**
- `cart.action` — `add` | `remove` | `quantity`
- `cart.item_count`
- `cart.has_out_of_stock`

**discovery.search.query.execute**
- `search.result_count`
- `search.has_filters`

**fulfillment.inventory.check**
- `inventory.available` — boolean
- `inventory.requested_qty`
- `inventory.available_qty` — catching partial stock is the whole point of this attribute

---

## At-Scale Governance (10+ teams)

### Registry
Maintain a `span-registry.yaml` in a central repo. PR review by the platform team when new spans are introduced.

```yaml
teams:
  checkout:
    owned_by: checkout-team
    domains: [payment, cart, order, confirmation]
  discovery:
    owned_by: discovery-team
    domains: [search, browse, recommendations, pdp]
```

### Cross-team spans
When one team's span calls into another team's domain, mark the boundary with `caller.team` and `callee.team` attributes. In your trace waterfall this immediately shows where team boundaries are crossed and which team owns the slow part.

### Key rules
- Span **names** are low-cardinality — never embed IDs, put them in attributes
- Span **attributes** are where all variable data lives
- `outcome` is required and is your primary alerting signal
- For long background tasks, use span events rather than many tiny child spans

---

## Span Termination States

How a span ends is as important as how it's named — termination state is a primary signal for alerting and funnel analysis.

### The three states

**Success** — the operation completed as expected. No error code needed, just end the span normally.

**Failure** — the operation failed due to a system or network error. Something went wrong that the user couldn't control — payment provider timeout, inventory service unavailable, token refresh rejected.

**User abandon** — the user deliberately stopped the operation before it completed. This is distinct from failure and is a critical ecommerce signal. A high abandon rate on `checkout.payment.authorize` means users are seeing the payment screen and leaving — that's a conversion problem, not a technical one.

### Ecommerce examples

| Span | Success | Failure | User Abandon |
|---|---|---|---|
| `checkout.order.submit` | Order placed | Payment provider error | User pressed back or closed app |
| `checkout.payment.authorize` | Auth approved | Gateway timeout | User switched payment method mid-flow |
| `cart.update` | Item added | Stock reservation failed | — |
| `discovery.search.query.execute` | Results returned | Search service error | User cleared query before results loaded |
| `account.auth.token.refresh` | Token refreshed | Refresh token expired | — |

User abandon is most meaningful on multi-step flows where the user has to wait — payment auth, address validation, order submission. It's less relevant for instant operations like `cart.update`.

### Important Embrace constraints to flag when naming

These limits affect naming decisions and should be shared with teams early:

| Platform | Max span name length | Max attributes per span |
|---|---|---|
| iOS | 128 characters | 100 |
| Android | 128 characters | 100 |
| React Native | 128 characters | 100 |

- The `emb-` and `emb.` prefixes are reserved by Embrace — never use them in span names or attribute keys
- Span names are case-sensitive across all platforms
- Dotted attribute keys are the norm and are encouraged — follow the OpenTelemetry convention of `domain.subdomain.field` (e.g. `cart.item_count`, `payment.method`). The SDKs use dotted keys internally (e.g. `emb.app.version`), so there is no alphanumeric-only restriction
- Keep attribute keys within the 128-character key-length limit

Deeply nested names should be checked against this limit before being added to the registry.

---

## Adapting to Other Verticals

When helping a non-ecommerce team, apply the same structure:

1. Map their product to **teams** and **domains**
2. Identify the 8–10 spans that cover their critical user journeys
3. For each span, ask: "What would a developer need to know at 2am to debug this?"
4. Those answers become tier 2 attributes

Common non-ecommerce domains: `auth`, `media`, `messaging`, `feed`, `payments`, `search`, `onboarding`.
