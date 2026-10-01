import '@mantine/core/styles.css';
import '@mantine/notifications/styles.css';
import '../public/main.css'
import {StrictMode} from 'react'
import {createRoot} from 'react-dom/client'
import {MantineProvider} from '@mantine/core';
import {Root} from "./ui/Root.tsx";

createRoot(document.getElementById('root')!).render(
    <StrictMode>
        <MantineProvider defaultColorScheme="light">
            <Root/>
        </MantineProvider>
    </StrictMode>,
)
