import {ImageInfo} from "../dto/imageInfo.ts";

const backendUrl = import.meta.env.VITE_BACKEND_URL

export type ConnectorState = "loading" | "error" | "ok"
export type SetConnectorState = (s: ConnectorState) => void

export const addImage = async (
    file: File,
    setConnectorState: SetConnectorState
) => {
    setConnectorState("loading")

    try {
        const formData = new FormData()
        formData.append("file", file)

        const res = await fetch(`${backendUrl}`, {
            method: "POST",
            body: formData
        })

        if (!res.ok) {
            throw new Error(`HTTP ${res.status}`)
        }
        setConnectorState("ok")
        return true
    } catch(e) {
        setConnectorState("error")
        console.log(`error: ${e}`)
        return false
    }
}

export const getImageList = async (
    consumeData: (v: ImageInfo[]) => void,
    setConnectorState: (s:ConnectorState) => void

) => {
    setConnectorState("loading")

    const controller = new AbortController();

    try {
        const res = await fetch(`${backendUrl}`, {signal: controller.signal});
        if (!res.ok) {
            throw new Error(`HTTP ${res.status}`)
        }
        const result: ImageInfo[] = await res.json()
        consumeData(result)
        setConnectorState("ok")
    } catch(e) {
        setConnectorState("error")
        console.log(`error: ${e}`)
    }

    return () => controller.abort();
}