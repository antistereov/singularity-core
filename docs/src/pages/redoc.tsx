import useDocusaurusContext from "@docusaurus/useDocusaurusContext";
import Layout from "@theme/Layout";
import {RedocStandalone} from "redoc";

export default function ApiPage() {
    const {siteConfig} = useDocusaurusContext();
    const specUrl = siteConfig.customFields.openApiUrl as string;
    return (
        <Layout
            title={`Hello from ${siteConfig.title}`}
            description="Description will go into a meta tag in <head />">
            <main>
                <RedocStandalone
                    specUrl={specUrl}
                    options={{
                        nativeScrollbars: true,
                        theme: { colors: { primary: { main: '#6366f1' } } }
                    }}
                />
            </main>
        </Layout>
    );
}
