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
          id: "api/change-preferred-method",
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
        {
          type: "doc",
          id: "api/recover-from-totp",
          label: "Recover From TOTP",
          className: "api-method post",
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
      label: "User Settings",
      link: {
        type: "generated-index",
        title: "User Settings",
        slug: "/category/api/user-settings",
      },
      items: [
        {
          type: "doc",
          id: "api/change-user",
          label: "changeUser",
          className: "api-method put",
        },
        {
          type: "doc",
          id: "api/delete",
          label: "delete",
          className: "api-method delete",
        },
        {
          type: "doc",
          id: "api/change-password",
          label: "changePassword",
          className: "api-method put",
        },
        {
          type: "doc",
          id: "api/change-email",
          label: "changeEmail",
          className: "api-method put",
        },
        {
          type: "doc",
          id: "api/set-avatar",
          label: "setAvatar",
          className: "api-method put",
        },
        {
          type: "doc",
          id: "api/delete-avatar",
          label: "deleteAvatar",
          className: "api-method delete",
        },
      ],
    },
    {
      type: "category",
      label: "Users",
      link: {
        type: "generated-index",
        title: "Users",
        slug: "/category/api/users",
      },
      items: [
        {
          type: "doc",
          id: "api/get-user",
          label: "Get currently authenticated user",
          className: "api-method get",
        },
        {
          type: "doc",
          id: "api/get-avatar",
          label: "getAvatar",
          className: "api-method get",
        },
      ],
    },
    {
      type: "category",
      label: "tag-controller",
      link: {
        type: "generated-index",
        title: "tag-controller",
        slug: "/category/api/tag-controller",
      },
      items: [
        {
          type: "doc",
          id: "api/find-tag-by-key",
          label: "findTagByKey",
          className: "api-method get",
        },
        {
          type: "doc",
          id: "api/update-tag",
          label: "updateTag",
          className: "api-method put",
        },
        {
          type: "doc",
          id: "api/delete-tag",
          label: "deleteTag",
          className: "api-method delete",
        },
        {
          type: "doc",
          id: "api/find-tag-by-key-contains",
          label: "findTagByKeyContains",
          className: "api-method get",
        },
        {
          type: "doc",
          id: "api/create-tag",
          label: "createTag",
          className: "api-method post",
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
          id: "api/invite-user",
          label: "inviteUser",
          className: "api-method post",
        },
        {
          type: "doc",
          id: "api/accept-invitation",
          label: "acceptInvitation",
          className: "api-method post",
        },
        {
          type: "doc",
          id: "api/create-article",
          label: "createArticle",
          className: "api-method post",
        },
        {
          type: "doc",
          id: "api/get-extended-access-details",
          label: "getExtendedAccessDetails",
          className: "api-method get",
        },
      ],
    },
    {
      type: "category",
      label: "admin-controller",
      link: {
        type: "generated-index",
        title: "admin-controller",
        slug: "/category/api/admin-controller",
      },
      items: [
        {
          type: "doc",
          id: "api/rotate-keys",
          label: "rotateKeys",
          className: "api-method post",
        },
        {
          type: "doc",
          id: "api/get-all-users",
          label: "getAllUsers",
          className: "api-method get",
        },
        {
          type: "doc",
          id: "api/rotation-ongoing",
          label: "rotationOngoing",
          className: "api-method get",
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
        {
          type: "doc",
          id: "api/get-latest-articles",
          label: "getLatestArticles",
          className: "api-method get",
        },
      ],
    },
    {
      type: "category",
      label: "local-file-storage-controller",
      link: {
        type: "generated-index",
        title: "local-file-storage-controller",
        slug: "/category/api/local-file-storage-controller",
      },
      items: [
        {
          type: "doc",
          id: "api/serve-file",
          label: "serveFile",
          className: "api-method get",
        },
      ],
    },
    {
      type: "category",
      label: "invitation-controller",
      link: {
        type: "generated-index",
        title: "invitation-controller",
        slug: "/category/api/invitation-controller",
      },
      items: [
        {
          type: "doc",
          id: "api/delete-invitation",
          label: "deleteInvitation",
          className: "api-method delete",
        },
      ],
    },
  ],
};

export default sidebar.apisidebar;
