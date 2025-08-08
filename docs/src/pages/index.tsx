import React, {ReactNode, useEffect} from 'react';
import useDocusaurusContext from "@docusaurus/useDocusaurusContext";
import Layout from "@theme/Layout";

import style from './index.module.css';
import {useHistory} from "@docusaurus/router";

export default function SingularityCorePage() {
    const history = useHistory();

    useEffect(() => {
        history.replace('/docs/intro');
    }, [history]);

    return null;
}
