The unformatted system rules document converted into Markdown follows below.

---

# SYSTEM RULES - DEFINITIVE REFERENCE

## 1. Hierarchy & State Transitions (Synced Users Only)

### 1.1 Canonical Chain

```text
MEMBERS_NEW → MEMBERS_ACTIVE → MEMBERS_TRUSTED → MODERATORS_PEER → MODERATORS_PROFESSIONAL → ADMINISTRATORS → SUPER_ADMINISTRATORS

```

### 1.2 Single-Step Only

* **Promotions:** Must move exactly **ONE** level up at a time.
* **Demotions:** Must move exactly **ONE** level down at a time.
* **Multi-level jumps:** Blocked.

### 1.3 Professional Exception (Promotion Only)

* `MODERATORS_PROFESSIONAL` can be assigned directly from **ANY LOWER** tier.
* **Valid:** `MEMBERS_NEW`, `MEMBERS_ACTIVE`, `MEMBERS_TRUSTED`, `MODERATORS_PEER` → `MODERATORS_PROFESSIONAL`
* **Invalid:** `ADMINISTRATORS` → `MODERATORS_PROFESSIONAL` (demotion)
* **Invalid:** `SUPER_ADMINISTRATORS` → `MODERATORS_PROFESSIONAL` (demotion)

### 1.4 Admin to Moderator Peer Demotion Exception

* `ADMINISTRATORS` **can** demote directly to `MODERATORS_PEER`, skipping `MODERATORS_PROFESSIONAL`.
* `SUPER_ADMINISTRATORS` **cannot** skip levels. Path required: `SUPER_ADMINISTRATORS` → `ADMINISTRATORS` → `MODERATORS_PEER`.
* **Invalid:** `MODERATORS_PEER` → `ADMINISTRATORS` (must go through `MOD_PRO` first).

---

## 2. Self-Modification Restrictions (Synced Users Only)

### 2.1 Superadmins

* Cannot promote themselves.
* Can demote themselves (subject to **Rule 4.1**).
* Cannot disable themselves.

### 2.2 Regular Admins

* Cannot promote themselves.
* Can demote themselves.
* Cannot disable themselves.

### 2.3 Moderators

* Self-modification rules deferred.
* Currently retain default capabilities.

---

## 3. Peer Protection (Synced Users Only)

### 3.1 Superadmins

* Cannot modify other superadmins.
* **Exception:** Self-demotion is exempt.

### 3.2 Admins

* Cannot modify other admins.
* **Exception:** Self-demotion is exempt.

### 3.3 Moderators

* Peer modification limits deferred.
* Currently lack admin permissions.

---

## 4. System Integrity (Synced Users Only)

### 4.1 At Least One Superadmin

* A minimum of one active superadmin must exist at all times.
* Any operation that would result in zero active superadmins is blocked.

### 4.2 Last Superadmin is Immutable

* The last remaining active superadmin becomes completely immutable.
* Cannot be demoted (even by self-demotion).
* Cannot be disabled.

---

## 5. Creation & Invite Management (Pending Users Only)

### 5.1 New Users Only

* New users can only be created with: `MEMBERS_NEW` or `MODERATORS_PROFESSIONAL`.
* Anything else is blocked.

### 5.2 Reissue/Update Pending

* Can freely move between `MEMBERS_NEW` and `MODERATORS_PROFESSIONAL`.
* No admin tiers (`ADMINISTRATORS` or `SUPER_ADMINISTRATORS`) can be assigned.

### 5.3 No Admin Tiers

* Pending users cannot be assigned administrative tiers.
* Attempting to assign `ADMINISTRATORS` or `SUPER_ADMINISTRATORS` is blocked.

### 5.4 Correction Flow

* Admin/Superadmin can correct improperly assigned admin tiers.
* Must correct back to: `MEMBERS_NEW` or `MODERATORS_PROFESSIONAL`.

---

## 6. Deferred Behaviors

### 6.1 First-User Auto-Promotion

* The "First User is automatic Superadmin" configuration is disabled.
* The system will not auto-promote the first registering user.

---

## 7. Group Assignment Permissions

### 7.1 MODERATORS_PROFESSIONAL Assignment

* Only superadmins can directly assign `MOD_PRO` during:
* Creation
* Reissue
* Pending updates



### 7.2 Regular Admin Promotion to MOD_PRO

* Regular admins **can** promote synced users to `MOD_PRO`.
* Must follow **Rule 1.2** (one level up from `MOD_PEER`).

### 7.3 Admin Tiers Assignment

* Only superadmins can assign:
* `ADMINISTRATORS`
* `SUPER_ADMINISTRATORS`



---

## Rule Mappings by Operation

| Operation | Rules That Apply |
| --- | --- |
| **CREATE USER** | 5.1, 7.1 |
| **REISSUE INVITATION** | 5.2, 5.3, 5.4, 7.1 |
| **UPDATE PENDING INVITE** | 5.3, 5.4, 7.1 |
| **UPDATE SYNCED USER** | 1.1, 1.2, 1.3, 1.4, 2.1, 2.2, 3.1, 3.2, 4.1, 4.2, 7.2, 7.3 |
| **REVOKE INVITATION** | State checks only |

---

## Quick Reference: What's Allowed and What's Blocked

### Pending Users

| Action | Allowed? |
| --- | --- |
| Create as `MEMBERS_NEW` | ✅ |
| Create as `MODERATORS_PROFESSIONAL` (Superadmin only) | ✅ |
| Reissue to `MEMBERS_NEW` | ✅ |
| Reissue to `MODERATORS_PROFESSIONAL` (Superadmin only) | ✅ |
| Reissue to `ADMINISTRATORS` | ❌ |
| Reissue to `MEMBERS_ACTIVE` | ❌ |
| Correct from admin tier to `MEMBERS_NEW` | ✅ |
| Correct from admin tier to `MODERATORS_PROFESSIONAL` (Superadmin only) | ✅ |

### Synced Users

| Action | Allowed? |
| --- | --- |
| Promote one level up | ✅ |
| Demote one level down | ✅ |
| Skip levels (two+) up | ❌ |
| Skip levels (two+) down | ❌ |
| Direct to `MOD_PRO` from any lower tier | ✅ |
| Demote `ADMIN` → `MOD_PRO` | ❌ |
| Demote `ADMIN` → `MOD_PEER` | ✅ |
| Promote `MOD_PEER` → `ADMIN` | ❌ |
| Self-promotion | ❌ |
| Self-demotion | ✅ |
| Self-disabling | ❌ |
| Self-update (same group) | ✅ |
| Modify other superadmin | ❌ |
| Modify other admin | ❌ |

---

## Enforcement Locations

| Rule | Enforced In |
| --- | --- |
| **1.1 Canonical Chain** | `GroupPath.getHierarchy()` |
| **1.2 Single-Step Only** | `GroupPath.isValidTransition()` |
| **1.3 Professional Exception** | `GroupPath.isValidTransition()` + `validateHierarchyProgression()` |
| **1.4 Admin → MOD_PEER Demotion** | `GroupPath.isValidTransition()` |
| **2.1, 2.2 Self-Modification** | `validateSelfModification()` |
| **3.1, 3.2 Peer Protection** | `validateSameLevelProtection()` |
| **4.1, 4.2 System Integrity** | `validateLastUserSafeguard()` |
| **5.1, 5.2, 5.3, 5.4 Pending Users** | `validateNewInviteGroup()`, `validatePendingInviteGroupTransition()` |
| **7.1, 7.2, 7.3 Group Assignment** | `validateGroupAssignmentPermission()` |

> **Note:** This document reflects the current implementation. The system is designed to be intuitive and self-documenting through context-aware dropdowns and clear feedback, so this markdown is a supplementary reference.