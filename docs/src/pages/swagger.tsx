import SwaggerUI from "swagger-ui-react"
import "swagger-ui-react/swagger-ui.css"
import useDocusaurusContext from "@docusaurus/useDocusaurusContext";
import Layout from "@theme/Layout";


export default function ApiPage() {
    const {siteConfig} = useDocusaurusContext();
    const openapiUrl = siteConfig.customFields.openApiUrl
    return (
        <Layout
            title={`Hello from ${siteConfig.title}`}
            description="Description will go into a meta tag in <head />">
            <main>
                <SwaggerUI
                    url={openapiUrl}
                    docExpansion="list"
                    defaultModelsExpandDepth={-1}
                    deepLinking={true}
                />
            </main>
        </Layout>
    );
}
