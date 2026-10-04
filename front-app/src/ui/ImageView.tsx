import * as React from "react";
import {useState} from "react";
import type {ImageInfo} from "../dto/imageInfo.ts";
import {Image, Modal} from "@mantine/core";

const backendUrl = import.meta.env.VITE_BACKEND_URL

type Props = {
    imageInfo: ImageInfo;
}

export const ImageView: React.FC<Props> = ({imageInfo}) => {

    const [showFull, setShowFull] = useState<boolean>(false)

    return <>
        <div className={"imageView"}>
            <Image
                style={{cursor: "pointer"}}
                onClick={() => setShowFull(true)}
                src={`${backendUrl}/${imageInfo.id}/thumbnail`}
            />
        </div>
        <Modal
            size={"xl"}
            title="Image Details"
            trapFocus={false}
            zIndex={8000}
            opened={showFull}
            onClose={() => setShowFull(false)}
            withinPortal={true}
        >
            <Image
                fit={"contain"}
                src={`${backendUrl}/${imageInfo.id}/original`}
            />
        </Modal>
    </>
}