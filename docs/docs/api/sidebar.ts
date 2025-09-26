import type { SidebarsConfig } from "@docusaurus/plugin-content-docs";

const sidebar: SidebarsConfig = {
  apisidebar: [
    {
      type: "doc",
      id: "api/singularity-api",
    },
    {
      type: "category",
      label: "Authentication",
      link: {
        type: "generated-index",
        title: "Authentication",
        slug: "/category/api/authentication",
      },
      items: [
        {
          type: "doc",
          id: "api/register",
          label: "Register",
          className: "api-method post",
        },
        {
          type: "doc",
          id: "api/login",
          label: "Login",
          className: "api-method post",
        },
        {
          type: "doc",
          id: "api/logout",
          label: "Logout",
          className: "api-method post",
        },
        {
          type: "doc",
          id: "api/refresh-access-token",
          label: "Refresh Access Token",
          className: "api-method post",
        },
        {
          type: "doc",
          id: "api/step-up",
          label: "Step-Up",
          className: "api-method post",
        },
        {
          type: "doc",
          id: "api/get-authentication-status",
          label: "Get Authentication Status",
          className: "api-method get",
        },
        {
          type: "doc",
          id: "api/send-password-reset-email",
          label: "Send Password Reset Email",
          className: "api-method post",
        },
        {
          type: "doc",
          id: "api/reset-password",
          label: "Reset Password",
          className: "api-method post",
        },
        {
          type: "doc",
          id: "api/get-remaining-password-reset-cooldown",
          label: "Get Remaining Password Reset Cooldown",
          className: "api-method get",
        },
        {
          type: "doc",
          id: "api/send-email-verification-email",
          label: "Send Email Verification Email",
          className: "api-method post",
        },
        {
          type: "doc",
          id: "api/verify-email",
          label: "Verify Email",
          className: "api-method post",
        },
        {
          type: "doc",
          id: "api/get-remaining-email-verification-cooldown",
          label: "Get Remaining Email Verification Cooldown",
          className: "api-method get",
        },
      ],
    },
    {
      type: "category",
      label: "Two-Factor Authentication",
      link: {
        type: "generated-index",
        title: "Two-Factor Authentication",
        slug: "/category/api/two-factor-authentication",
      },
      items: [
        {
          type: "doc",
          id: "api/complete-login",
          label: "Complete Login",
          className: "api-method post",
        },
        {
          type: "doc",
          id: "api/complete-step-up",
          label: "Complete Step-Up",
          className: "api-method post",
        },
        {
          type: "doc",
          id: "api/change-preferred-two-factor-method",
          label: "Change Preferred 2FA Method",
          className: "api-method post",
        },
        {
          type: "doc",
          id: "api/get-totp-setup-details",
          label: "Get TOTP Setup Details",
          className: "api-method get",
        },
        {
          type: "doc",
          id: "api/enable-totp-as-two-factor-method",
          label: "Enable TOTP as 2FA Method",
          className: "api-method post",
        },
        {
          type: "doc",
          id: "api/disable-totp-as-two-factor-method",
          label: "Disable TOTP as 2FA Method",
          className: "api-method delete",
        },
        {
          type: "doc",
          id: "api/recover-from-totp",
          label: "Recover From TOTP",
          className: "api-method post",
        },
        {
          type: "doc",
          id: "api/send-email-two-factor-code",
          label: "Send Email 2FA Code",
          className: "api-method post",
        },
        {
          type: "doc",
          id: "api/enable-email-as-two-factor-method",
          label: "Enable Email as 2FA Method",
          className: "api-method post",
        },
        {
          type: "doc",
          id: "api/disable-email-as-two-factor-method",
          label: "Disable Email as 2FA Method",
          className: "api-method delete",
        },
        {
          type: "doc",
          id: "api/get-remaining-email-two-factor-cooldown",
          label: "Get Remaining Email 2FA Code Cooldown",
          className: "api-method get",
        },
      ],
    },
    {
      type: "category",
      label: "OAuth2",
      link: {
        type: "generated-index",
        title: "OAuth2",
        slug: "/category/api/o-auth-2",
      },
      items: [
        {
          type: "doc",
          id: "api/get-identity-providers",
          label: "Get Identity Providers",
          className: "api-method get",
        },
        {
          type: "doc",
          id: "api/delete-identity-provider",
          label: "Delete Identity Provider",
          className: "api-method delete",
        },
        {
          type: "doc",
          id: "api/add-password-authentication",
          label: "Add Password Authentication",
          className: "api-method post",
        },
        {
          type: "doc",
          id: "api/generate-o-auth-2-provider-connection-token",
          label: "Generate OAuth2ProviderConnectionToken",
          className: "api-method post",
        },
      ],
    },
    {
      type: "category",
      label: "Sessions",
      link: {
        type: "generated-index",
        title: "Sessions",
        slug: "/category/api/sessions",
      },
      items: [
        {
          type: "doc",
          id: "api/get-active-sessions",
          label: "Get Active Sessions",
          className: "api-method get",
        },
        {
          type: "doc",
          id: "api/delete-all-sessions",
          label: "Delete All Sessions",
          className: "api-method delete",
        },
        {
          type: "doc",
          id: "api/delete-session",
          label: "Delete Session",
          className: "api-method delete",
        },
        {
          type: "doc",
          id: "api/generate-session-token",
          label: "Generate SessionToken",
          className: "api-method post",
        },
      ],
    },
    {
      type: "category",
      label: "Roles",
      link: {
        type: "generated-index",
        title: "Roles",
        slug: "/category/api/roles",
      },
      items: [
        {
          type: "doc",
          id: "api/create-guest-account",
          label: "Create Guest Account",
          className: "api-method post",
        },
        {
          type: "doc",
          id: "api/convert-guest-to-user",
          label: "Convert Guest To User",
          className: "api-method post",
        },
        {
          type: "doc",
          id: "api/grant-admin-permissions",
          label: "Grant Admin Permissions",
          className: "api-method post",
        },
        {
          type: "doc",
          id: "api/revoke-admin-permissions",
          label: "Revoke Admin Permissions",
          className: "api-method delete",
        },
      ],
    },
    {
      type: "category",
      label: "Groups",
      link: {
        type: "generated-index",
        title: "Groups",
        slug: "/category/api/groups",
      },
      items: [
        {
          type: "doc",
          id: "api/get-groups",
          label: "Get Groups",
          className: "api-method get",
        },
        {
          type: "doc",
          id: "api/create-group",
          label: "Create Group",
          className: "api-method post",
        },
        {
          type: "doc",
          id: "api/get-group-by-key",
          label: "Get Group By Key",
          className: "api-method get",
        },
        {
          type: "doc",
          id: "api/update-group",
          label: "Update Group",
          className: "api-method put",
        },
        {
          type: "doc",
          id: "api/delete-group",
          label: "Delete Group",
          className: "api-method delete",
        },
        {
          type: "doc",
          id: "api/add-member-to-group",
          label: "Add Member to Group",
          className: "api-method post",
        },
        {
          type: "doc",
          id: "api/remove-member-from-group",
          label: "Remove Member from Group",
          className: "api-method delete",
        },
      ],
    },
    {
      type: "category",
      label: "Profile Management",
      link: {
        type: "generated-index",
        title: "Profile Management",
        slug: "/category/api/profile-management",
      },
      items: [
        {
          type: "doc",
          id: "api/get-authorized-user",
          label: "Get User",
          className: "api-method get",
        },
        {
          type: "doc",
          id: "api/update-authorized-user",
          label: "Update User",
          className: "api-method put",
        },
        {
          type: "doc",
          id: "api/delete-authorized-user",
          label: "Delete User",
          className: "api-method delete",
        },
        {
          type: "doc",
          id: "api/change-email-of-authorized-user",
          label: "Change Email",
          className: "api-method put",
        },
        {
          type: "doc",
          id: "api/change-password-of-authorized-user",
          label: "Change Password",
          className: "api-method put",
        },
        {
          type: "doc",
          id: "api/set-avatar-of-authorized-user",
          label: "Update Avatar",
          className: "api-method put",
        },
        {
          type: "doc",
          id: "api/delete-avatar-of-authorized-user",
          label: "Delete Avatar",
          className: "api-method delete",
        },
      ],
    },
    {
      type: "category",
      label: "Tags",
      link: {
        type: "generated-index",
        title: "Tags",
        slug: "/category/api/tags",
      },
      items: [
        {
          type: "doc",
          id: "api/find-tags",
          label: "Find Tags",
          className: "api-method get",
        },
        {
          type: "doc",
          id: "api/create-tag",
          label: "Create Tag",
          className: "api-method post",
        },
        {
          type: "doc",
          id: "api/find-tag-by-key",
          label: "Find Tag By Key",
          className: "api-method get",
        },
        {
          type: "doc",
          id: "api/update-tag",
          label: "Update Tag",
          className: "api-method put",
        },
        {
          type: "doc",
          id: "api/delete-tag",
          label: "Delete Tag",
          className: "api-method delete",
        },
      ],
    },
    {
      type: "category",
      label: "Invitations",
      link: {
        type: "generated-index",
        title: "Invitations",
        slug: "/category/api/invitations",
      },
      items: [
        {
          type: "doc",
          id: "api/invite-user",
          label: "Invite User",
          className: "api-method post",
        },
        {
          type: "doc",
          id: "api/accept-invitation",
          label: "Accept Invitation",
          className: "api-method post",
        },
        {
          type: "doc",
          id: "api/delete-invitation",
          label: "Delete Invitation",
          className: "api-method delete",
        },
      ],
    },
    {
      type: "category",
      label: "Security",
      link: {
        type: "generated-index",
        title: "Security",
        slug: "/category/api/security",
      },
      items: [
        {
          type: "doc",
          id: "api/rotate-secret-keys",
          label: "Trigger Secret Rotation",
          className: "api-method post",
        },
        {
          type: "doc",
          id: "api/get-secret-key-rotation-status",
          label: "Get Secret Rotation Status",
          className: "api-method get",
        },
      ],
    },
    {
      type: "category",
      label: "Managing Users",
      link: {
        type: "generated-index",
        title: "Managing Users",
        slug: "/category/api/managing-users",
      },
      items: [
        {
          type: "doc",
          id: "api/get-user-by-id",
          label: "Get User By ID",
          className: "api-method get",
        },
        {
          type: "doc",
          id: "api/delete-user-by-id",
          label: "Delete User By ID",
          className: "api-method delete",
        },
        {
          type: "doc",
          id: "api/get-users",
          label: "Get Users",
          className: "api-method get",
        },
      ],
    },
    {
      type: "category",
      label: "content-management-controller",
      link: {
        type: "generated-index",
        title: "content-management-controller",
        slug: "/category/api/content-management-controller",
      },
      items: [
        {
          type: "doc",
          id: "api/change-visibility",
          label: "changeVisibility",
          className: "api-method put",
        },
        {
          type: "doc",
          id: "api/set-trusted-state",
          label: "setTrustedState",
          className: "api-method put",
        },
        {
          type: "doc",
          id: "api/change-tags",
          label: "changeTags",
          className: "api-method put",
        },
        {
          type: "doc",
          id: "api/get-extended-access-details",
          label: "getExtendedAccessDetails",
          className: "api-method get",
        },
        {
          type: "doc",
          id: "api/delete-content-by-key",
          label: "deleteContentByKey",
          className: "api-method delete",
        },
      ],
    },
    {
      type: "category",
      label: "article-management-controller",
      link: {
        type: "generated-index",
        title: "article-management-controller",
        slug: "/category/api/article-management-controller",
      },
      items: [
        {
          type: "doc",
          id: "api/change-summary",
          label: "changeSummary",
          className: "api-method put",
        },
        {
          type: "doc",
          id: "api/change-state",
          label: "changeState",
          className: "api-method put",
        },
        {
          type: "doc",
          id: "api/change-image",
          label: "changeImage",
          className: "api-method put",
        },
        {
          type: "doc",
          id: "api/change-header",
          label: "changeHeader",
          className: "api-method put",
        },
        {
          type: "doc",
          id: "api/change-content",
          label: "changeContent",
          className: "api-method put",
        },
        {
          type: "doc",
          id: "api/create-article",
          label: "createArticle",
          className: "api-method post",
        },
      ],
    },
    {
      type: "category",
      label: "article-controller",
      link: {
        type: "generated-index",
        title: "article-controller",
        slug: "/category/api/article-controller",
      },
      items: [
        {
          type: "doc",
          id: "api/get-articles",
          label: "getArticles",
          className: "api-method get",
        },
        {
          type: "doc",
          id: "api/get-article-by-key",
          label: "getArticleByKey",
          className: "api-method get",
        },
      ],
    },
    {
      type: "category",
      label: "file-metadata-controller",
      link: {
        type: "generated-index",
        title: "file-metadata-controller",
        slug: "/category/api/file-metadata-controller",
      },
      items: [
        {
          type: "doc",
          id: "api/get-files",
          label: "getFiles",
          className: "api-method get",
        },
        {
          type: "doc",
          id: "api/get-file-by-key",
          label: "getFileByKey",
          className: "api-method get",
        },
      ],
    },
  ],
};

export default sidebar.apisidebar;
