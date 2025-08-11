---
sidebar_position: 1
title: Intro
---

# Singularity

Welcome to the foundation for your next backend!
Build your app with everything from authentication to content management, ready out of the box. 🚀

Save time, ensure consistency, and focus on features — whether you're building an API, microservice, or full-stack app.

## ⚡ Why Use This?

- ✔ **Batteries Included:** authentication with 2FA and email verification, content management, file storage, and key rotation already set up.
- ✔ **Production-Ready by Default:** All components are built with real-world usage and scalability in mind.
- ✔ **Open & Extensible:** Contributions welcome! Let’s refine this into a toolkit others can benefit from too.

## 🔐 Features at a Glance

#### **Authentication & User Management**
- 🔒 JWT auth with refresh tokens, 2FA, secure HTTP-only cookies.
- 📧 Email verification with expiration and resend control.
- 🧑‍💻 Role-based user access with custom exceptions for better error handling.

#### **Data & Caching**
- 💾 MongoDB for persistence, Redis for caching and session storage.
- 🗂️ S3-based object storage abstraction with local fallback.

#### **Content Management**
- 🧩 Abstract base for content types with **built-in access control** (users, groups, roles).
- 🌍 **Multi-language support** out-of-the-box — store and serve content in multiple locales.
- 🏷️ Configurable tagging system for flexible content organization.
- 📝 Prebuilt `Article` class for instant publishing workflows.

#### **Security & Key Management**
- 🔑 Secret manager integration with **automated key rotation** for your secrets.

#### **Performance**
- ⚙️ Kotlin Coroutines for async flows.
- 🚦 Configurable IP and user-based rate limiting.
