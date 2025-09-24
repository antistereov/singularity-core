import {themes as prismThemes} from 'prism-react-renderer';
import type {Config} from '@docusaurus/types';
import type * as Preset from '@docusaurus/preset-classic';
import type * as OpenApiPlugin from "docusaurus-plugin-openapi-docs";

// This runs in Node.js - Don't use client-side code here (browser APIs, JSX...)

const config: Config = {
    title: 'Singularity',
    tagline: 'Your easy web starter for Kotlin and Spring',
    favicon: 'img/favicon.ico',

    // Future flags, see https://docusaurus.io/docs/api/docusaurus-config#future
    future: {
        v4: true, // Improve compatibility with the upcoming Docusaurus v4
    },

    // Set the production url of your site here
    url: 'https://singularity.stereov.io',
    // Set the /<baseUrl>/ pathname under which your site is served
    // For GitHub pages deployment, it is often '/<projectName>/'
    baseUrl: '/',

    // GitHub pages deployment config.
    // If you aren't using GitHub pages, you don't need these.
    organizationName: 'antistereov', // Usually your GitHub org/user name.
    projectName: 'singularity', // Usually your repo name.

    onBrokenLinks: 'throw',
    onBrokenMarkdownLinks: 'throw',

    // Even if you don't use internationalization, you can use this field to set
    // useful metadata like html lang. For example, if your site is Chinese, you
    // may want to replace "en" with "zh-Hans".
    i18n: {
        defaultLocale: 'en',
        locales: ['en'],
    },

    presets: [
        [
            '@docusaurus/preset-classic',
            {
                theme: {
                    customCss: './src/css/custom.css'
                },
                docs: {
                    sidebarPath: './sidebars.ts',
                    docItemComponent: "@theme/ApiItem"
                },
                blog: false,
            } satisfies Preset.Options
        ]
    ],
    plugins: [
        [
            'docusaurus-plugin-openapi-docs',
            {
                id: "api", // plugin id
                docsPluginId: "classic", // configured for preset-classic
                config: {
                    singularity: {
                        specPath: "static/openapi/openapi.yaml",
                        outputDir: "docs/api",
                        sidebarOptions: {
                            groupPathsBy: "tag",
                            categoryLinkSource: "auto",
                        },
                    } satisfies OpenApiPlugin.Options,
                }
            },
        ],
    ],
    themes: [
        "docusaurus-theme-openapi-docs"
    ],
    themeConfig: {
        // Replace with your project's social card
        image: 'img/social-card.png',
        colorMode: {
            respectPrefersColorScheme: true,
        },
        navbar: {
            title: 'Singularity',
            logo: {
                alt: 'My Site Logo',
                src: 'img/logo.svg',
            },
            items: [
                {
                    type: 'docSidebar',
                    sidebarId: 'guideSidebar',
                    position: 'left',
                    label: 'Docs',
                },
                {
                    href: 'https://github.com/antistereov/singularity-core',
                    label: 'GitHub',
                    position: 'right',
                },
            ],
        },
        footer: {
            style: 'dark',
            links: [
                {
                    title: 'Docs',
                    items: [
                        {
                            label: 'Singlarity',
                            to: '/docs/intro',
                        },
                        {
                            label: 'Spring',
                            href: 'https://spring.io'
                        }
                    ],
                },
                {
                    title: 'Social',
                    items: [
                        {
                            label: 'Instagram',
                            href: 'https://instagram.com/antistereov.coding',
                        },
                        {
                            label: 'X',
                            href: 'https://x.com/antistereov',
                        },
                        {
                            label: 'LinkedIn',
                            href: 'https://linkedin.com/in/antistereov'
                        }
                    ],
                },
                {
                    title: 'More',
                    items: [
                        {
                            label: 'My Website',
                            href: 'https://stereov.io',
                        },
                        {
                            label: 'GitHub',
                            href: 'https://github.com/antistereov',
                        }
                    ],
                },
            ],
            copyright: `Copyright © ${new Date().getFullYear()} Stereov Built with Docusaurus.`,
        },
        prism: {
            theme: prismThemes.github,
            darkTheme: prismThemes.dracula,
        }
    } satisfies Preset.ThemeConfig,
};

export default config;
