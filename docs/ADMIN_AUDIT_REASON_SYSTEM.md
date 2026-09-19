# ADMIN AUDIT REASON SYSTEM

## Overview

The admin audit reason system provides predefined reasons for admin-initiated
user changes. It ensures accountability and provides context for every action.

---

## 1. ACTION TYPE MAPPING

| `UserAuditAction` | Has Reasons? | Seed Data Exists? |
|-------------------|--------------|-------------------|
| `CREATED` | ✅ Yes | ✅ `SYSTEM_AUTO` |
| `SYNCED` | ❌ No | ❌ Not needed (system action) |
| `PROMOTED` | ✅ Yes | ✅ 4 reasons |
| `DEMOTED` | ✅ Yes | ✅ 3 reasons |
| `GROUP_CHANGED` | ✅ Yes | ✅ `ADMIN_CORRECTION` |
| `ENABLED` | ✅ Yes | ✅ `ACCOUNT_RECOVERY` |
| `DISABLED` | ✅ Yes | ✅ 3 reasons |
| `INVITE_REISSUED` | ✅ Yes | ✅ 5 reasons |
| `INVITE_REVOKED` | ✅ Yes | ✅ 4 reasons |

---

## 2. OPERATION ↔ REASON MAPPING

| Operation | Action Type | Reason Required? | Seed Reasons Available |
|-----------|-------------|------------------|------------------------|
| CREATE USER | `CREATED` | ❌ No (system) | `SYSTEM_AUTO` (not used) |
| REISSUE INVITATION | `INVITE_REISSUED` | ⚠️ Optional | `INVITE_EXPIRED`, `EMAIL_BOUNCED`, `USER_REQUEST_RESEND`, `ADMIN_CORRECTION_REISSUE`, `EMAIL_UPDATED` |
| UPDATE PENDING - Group | `GROUP_CHANGED` | ✅ Yes | `ADMIN_CORRECTION` |
| UPDATE PENDING - Enabled | `ENABLED` / `DISABLED` | ✅ Yes | `ACCOUNT_RECOVERY` / `TEMP_SUSPENSION`, `ANONYMIZED_RETAINED`, `ADMIN_MANUAL_LOCK` |
| UPDATE SYNCED - Group | `PROMOTED` / `DEMOTED` | ✅ Yes | 4 promotion reasons / 3 demotion reasons |
| UPDATE SYNCED - Enabled | `ENABLED` / `DISABLED` | ✅ Yes | `ACCOUNT_RECOVERY` / `TEMP_SUSPENSION`, `ANONYMIZED_RETAINED`, `ADMIN_MANUAL_LOCK` |
| REVOKE INVITATION | `INVITE_REVOKED` | ✅ Yes | `INVITE_CANCELLED`, `USER_REQUEST_REVOKE`, `DUPLICATE_INVITE`, `POLICY_VIOLATION_INVITE` |

---

## 3. VALIDATION FLOW CHECKLIST

| Step | Validation | Uses Seed Data? |
|------|------------|-----------------|
| 1 | Reason provided (template or custom) | ✅ Checks `reasonDefinitionId` or `customReason` |
| 2 | Reason definition exists | ✅ `findById(reasonDefinitionId)` |
| 3 | Reason matches action type | ✅ `validateReasonMatchesAction(reason.getActionType(), expectedAction)` |
| 4 | Audit log stores reason | ✅ `logAction(..., reasonDefinitionId, customReason)` |

---

## 4. ACTION DETERMINATION CHECKLIST

### Group Changes

| Operation | Current Group | Target Group | `forGroupChange` Returns | Valid Reason |
|-----------|---------------|--------------|--------------------------|--------------|
| Promote synced | `MEMBERS_NEW` | `MEMBERS_ACTIVE` | `PROMOTED` | ✅ Promotion reasons |
| Demote synced | `MEMBERS_ACTIVE` | `MEMBERS_NEW` | `DEMOTED` | ✅ Demotion reasons |
| Change pending | `MEMBERS_NEW` | `MODERATORS_PROFESSIONAL` | `GROUP_CHANGED` | ✅ `ADMIN_CORRECTION` |
| Correct pending | `ADMINISTRATORS` | `MEMBERS_NEW` | `GROUP_CHANGED` | ✅ `ADMIN_CORRECTION` |

### Enabled Changes

| Operation | Current Enabled | Target Enabled | `forEnabledChange` Returns | Valid Reason |
|-----------|-----------------|----------------|----------------------------|--------------|
| Enable user | `false` | `true` | `ENABLED` | ✅ `ACCOUNT_RECOVERY` |
| Disable user | `true` | `false` | `DISABLED` | ✅ `TEMP_SUSPENSION`, `ANONYMIZED_RETAINED`, `ADMIN_MANUAL_LOCK` |

---

## 5. EDGE CASES CHECKLIST

| Scenario | Action Type | Reason Required? | Seed Data Available? |
|----------|-------------|------------------|----------------------|
| Multi-change: Promote + Disable | `PROMOTED` + `DISABLED` | ✅ Both | ✅ Promotion + Disabled reasons |
| Multi-change: Demote + Enable | `DEMOTED` + `ENABLED` | ✅ Both | ✅ Demotion + Enabled reasons |
| Reissue with email change | `INVITE_REISSUED` | ⚠️ Optional | ✅ `EMAIL_UPDATED` |
| Reissue after bounce | `INVITE_REISSUED` | ⚠️ Optional | ✅ `EMAIL_BOUNCED` |
| Revoke for policy | `INVITE_REVOKED` | ✅ Yes | ✅ `POLICY_VIOLATION_INVITE` |
| System action (create) | `CREATED` | ❌ No | N/A |
| System action (sync) | `SYNCED` | ❌ No | N/A |

---

## 6. CONSTANT TOGGLE CHECKLIST

| `REQUIRE_REASON_FOR_ADMIN_CHANGES` | Behavior |
|-------------------------------------|----------|
| `true` | Reason required for all admin changes (except `CREATE`, `SYNCED`) |
| `false` | Reason optional for all operations |

**When `false`:**
- `validateReasonProvided` skips
- `validateReasonMatchesAction` still runs if reason provided
- Audit logs with null reasons

---

## 7. FRONTEND CHECKLIST

| Frontend Action | Fetches Reasons? | Sends Reason? |
|-----------------|------------------|---------------|
| Create user | ❌ No | ❌ No |
| Reissue invitation | ✅ `INVITE_REISSUED` | ⚠️ Optional |
| Update pending - group | ✅ `GROUP_CHANGED` | ✅ Yes |
| Update pending - enabled | ✅ `ENABLED` / `DISABLED` | ✅ Yes |
| Update synced - group | ✅ `PROMOTED` / `DEMOTED` | ✅ Yes |
| Update synced - enabled | ✅ `ENABLED` / `DISABLED` | ✅ Yes |
| Revoke invitation | ✅ `INVITE_REVOKED` | ✅ Yes |

---

## 8. DATABASE CHECKLIST

| Check | Status |
|-------|--------|
| `user_audit_reason_definitions` seeded | ✅ |
| All action types have at least one reason | ✅ |
| `is_active = true` for all seed data | ✅ |
| `sort_order` set for UI ordering | ✅ |
| Enum types match Java enums | ✅ |

---

## 9. TESTING CHECKLIST

| # | Test | Expected |
|---|------|----------|
| 1 | Reason required for group change | 400 if no reason |
| 2 | Reason required for enabled change | 400 if no reason |
| 3 | Reason matches action | 400 if mismatch |
| 4 | Valid reason accepted | 200 + audit logged |
| 5 | Custom reason accepted | 200 + audit logged |
| 6 | Both template and custom accepted | 200 + audit logged |
| 7 | Reason optional for reissue | 200 without reason |
| 8 | Reason required for revoke | 400 if no reason |
| 9 | System actions don't require reason | 200 (`CREATE`, `SYNCED`) |
| 10 | Constant toggle works | Behavior changes |

---

## 10. SEED DATA REFERENCE

### `user_audit_reason_definitions`

| Key | Description | Action Type | Sort Order |
|-----|-------------|-------------|------------|
| `EXCEPTIONAL_CONTRIBUTION` | Exceptional contribution to the community | `PROMOTED` | 10 |
| `TRUSTED_ESTABLISHED` | User has established trust over time | `PROMOTED` | 20 |
| `PROFESSIONAL_CREDENTIALS` | User has verified professional credentials | `PROMOTED` | 30 |
| `MODERATOR_NOMINATION` | Nominated by fellow moderators | `PROMOTED` | 40 |
| `POLICY_VIOLATION` | Violation of community guidelines | `DEMOTED` | 10 |
| `INACTIVITY` | Inactive for an extended period | `DEMOTED` | 20 |
| `REQUESTED_DEMOTION` | User requested demotion | `DEMOTED` | 30 |
| `TEMP_SUSPENSION` | Temporary suspension pending review | `DISABLED` | 10 |
| `ANONYMIZED_RETAINED` | Account deleted in Keycloak; local data anonymized | `DISABLED` | 50 |
| `ADMIN_MANUAL_LOCK` | Administrative manual account freeze | `DISABLED` | 60 |
| `ACCOUNT_RECOVERY` | Account recovered after verification | `ENABLED` | 10 |
| `ADMIN_CORRECTION` | Administrative correction | `GROUP_CHANGED` | 10 |
| `SYSTEM_AUTO` | System automated action | `CREATED` | 10 |
| `INVITE_EXPIRED` | Invitation expired | `INVITE_REISSUED` | 10 |
| `EMAIL_BOUNCED` | Original email bounced | `INVITE_REISSUED` | 20 |
| `USER_REQUEST_RESEND` | User requested resend | `INVITE_REISSUED` | 30 |
| `ADMIN_CORRECTION_REISSUE` | Administrative correction | `INVITE_REISSUED` | 40 |
| `EMAIL_UPDATED` | Recipient email address updated | `INVITE_REISSUED` | 50 |
| `INVITE_CANCELLED` | Invitation cancelled by admin | `INVITE_REVOKED` | 10 |
| `USER_REQUEST_REVOKE` | User requested cancellation | `INVITE_REVOKED` | 20 |
| `DUPLICATE_INVITE` | Duplicate invitation | `INVITE_REVOKED` | 30 |
| `POLICY_VIOLATION_INVITE` | Policy violation | `INVITE_REVOKED` | 40 |

---

## 11. FRONTEND CONTEXT-AWARE DROPDOWN (DEFERRED)

### The Challenge

`ActionType` alone is insufficient to determine reason group because:

| UserType | ActionType | Correct `UserAuditAction` |
|----------|------------|---------------------------|
| `SYNCED` | `PROMOTE` | `PROMOTED` |
| `SYNCED` | `DEMOTE` | `DEMOTED` |
| `PENDING` | `PROMOTE` | `GROUP_CHANGED` |
| `PENDING` | `DEMOTE` | `GROUP_CHANGED` |
| `PENDING` | `CORRECT` | `GROUP_CHANGED` |

### Possible Solutions

| Option | Approach |
|--------|----------|
| A | Add `UserType` to `AvailableGroup` |
| B | Add `UserAuditAction` directly to `AvailableGroup` |
| C | Frontend knows `UserType` from endpoint context |

**Status:** Deferred until frontend implementation.