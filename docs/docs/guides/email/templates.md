# Templates

# Configuration

:::note
This guide assumes familiarity with the [Spring Framework](https://spring.io).  
If you are new to Spring, we recommend starting with their [official guides](https://spring.io/quickstart) to get up to speed.
:::

The Singularity email module provides a way to send templated and translated emails. 
It uses a template builder with a base HTML template and separate templates for the email content. 
Translation is handled by a resource bundle, allowing for internationalization (i18n).

## Template Builder

The `TemplateBuilder` class is responsible for creating email templates. 
It supports loading templates from a resource file or a string, translating them, and replacing placeholders.

The `TemplateBuilder` can be used as follows:

1.  **Load a template:**

    * `TemplateBuilder.fromResource(templateResource: String)`: Loads a template from a file 
        located in the application's resources.
    * `TemplateBuilder.fromString(template: String)`: Creates a template from a given string.

2.  **Translate the template:**

    * `translate(translationResource: String, locale: Locale)`: 
        Translates the template by replacing `{{ key | translate }}` placeholders with values from a resource bundle, 
        based on the provided `locale`.

3.  **Replace placeholders:**

    * `replacePlaceholders(placeholders: Map<String, Any>)`: Replaces `{{ key }}` placeholders with values from a map.

4.  **Build the final template:**

    * `build()`: Returns the final, processed string.

## Email Templates

Email templates are located in the `resources/templates/mail` directory. 
The `base.html` file serves as the main layout for all emails.

### Base Template (`base.html`)

The `base.html` template contains the overall structure of the email, including the header, footer, and a content area. 
It includes placeholders for the email's `subject` and `content`, as well as several 
translated and untranslated placeholders that are populated by the `TemplateService`.

### Content Templates

Specific email content, such as a password reset or invitation, 
is defined in separate HTML files (e.g., `password_reset.html`) located in the same directory as `base.html`. 
These files contain the unique body of the email, which is then inserted into the `{{ content }}` 
placeholder of the base template.

The content templates can also contain their own placeholders. 
For example, `password_reset.html` has placeholders like `{{ password_reset.description.1 | translate }}` 
and `{{ reset_url }}`.

## Internationalization (i18n)

Translations are managed using Java resource bundles (`.properties` files). 
These files are located in the `resources/i18n/core` directory. 
The `email_en.properties`file provides English translations.

* **`{{ key | translate }}` placeholders:** These are replaced with the corresponding 
  value from the resource bundle based on the user's `Locale`.
* **File naming convention:** The resource bundle is named `i18n/core/email`. 
  Translations for different locales are stored in files with the format `email_<locale>.properties` 
  (e.g., `email_en.properties` for English).
* **Default behavior:** If a key is not found in the resource bundle, 
  the placeholder key itself will be used as the translation.

## Preconfigured Emails

All preconfigured emails use the [base template](#base-template-basehtml).

* **Email Verification**: Used to verify the user's email address. Learn more [here](../auth/authentication.md#email-verification).
* **Password Reset:** Used to verify the reset the user's password if they cannot remember it. Learn more [here](../auth/authentication.md#password-reset).
* **Email 2FA Code:** Contains the 2FA code if email as 2FA method is enabled. Learn more [here](../auth/two-factor.md#email).

## Usage Example

Here is a usage example for creating and sending a welcome email. 

### Step 1: Creating a Template

Create the file `welcome.html` file inside your resources in the directory in `templates/mail`.

```html
<div>
    <p>{{ welcome.description.1 | translate }}</p>
    <p>{{ welcome.description.2 | translate }}</p>
    <p>{{ welcome.description.3 | translate }}</p>
</div>
<a style="
    box-sizing: border-box;
    display: inline-block;
    width: 100%;
    padding: 0.75rem 1rem;
    font-size: 1rem;
    font-weight: 500;
    border: 0;
    border-radius: 0.375rem;
    cursor: pointer;
    background-color: {{ primary_color }};
    color: white;
    text-decoration: none;
    text-align: center;"
    href="{{ get_started_url }}"
>
    <strong>{{ welcome.get_started | translate }}</strong>
</a>
<div>
    <p>{{ welcome.description.4 | translate }}</p>
</div>
```

### Step 2: Creating Translations

Create the file `email_en.properties` file inside your resources in the directory `i18n/custom/email`.
You can add more languages by changing the locale, e.g. `email_de.properties` for German.

```properties
welcome.subject=Welcome to {{ app_name }}!
welcome.description.1=Hey {{ name }},
welcome.description.2=We are so excited to have you as a new user!
welcome.description.3=To get started, please log in to your account.
welcome.description.4=If you have any questions, feel free to contact us.
welcome.get_started=Get started
```

This example assumes a `welcome.html` content template and 
uses the `EmailTemplateService` and other components to generate and send the email to a new user.

```kotlin
// In your WelcomeService.kt
@Service
class WelcomeService(
    private val emailTemplateService: EmailTemplateService,
    private val emailService: EmailService,
    private val templateService: TemplateService,
    private val translateService: TranslateService,
    private val appProperties: AppProperties
) {

    private val logger = KotlinLogging.logger {}
    // The location of your translations
    val resourceBundle = "i18n/custom/email"
    // The location of your template
    val templateDir = "templates/mail"
    // The location of the base template
    // If you want to use the base template provided by Singularity, just use templates/mail/base.html.
    val baseTemplatePath = "templates/mail/base.html"

    suspend fun sendWelcomeEmail(to: String, name: String, locale: Locale?): Result<MimeMessage, Exception> = coroutineBinding {
        logger.debug { "Sending welcome email to $to" }

        // If no locale is specified, the application's default locale will be used.
        val actualLocale = locale ?: appProperties.locale

        // The name of your template
        val slug = "welcome"
        val templatePath = "$templateDir/$slug.html"

        // Prepare content placeholders
        val placeholders = mapOf(
            "name" to name
        )

        // The following translations are a good fit for a welcome email based on the provided email_en.properties file
        // "email_verification.subject" = "Just one more step"
        // "email_verification.description.2" = "Welcome to {{ app_name }} - we’re excited to have you on board!"
        
        // Load the welcome content template and process its placeholders
        val content = TemplateBuilder.fromResource(templatePath)
            .translate(resourceBundle, actualLocale)
            .replacePlaceholders(templateService.getPlaceholders(placeholders))
            .build()
            .bind()

        // Get the translated subject
        val subject = translateService.translateResourceKey(
            TranslateKey("$slug.subject"),
            resourceBundle,
            actualLocale
        )

        // Create the final, complete email template
        val finalTemplate = emailTemplateService.createTemplate(subject, content, actualLocale)
            .bind()

        // Send the email
        emailService.sendEmail(to, subject, finalTemplate, actualLocale)
            .bind()
    }
}
```