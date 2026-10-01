import * as React from "react";
import type {ImageInfo} from "../dto/imageInfo.ts";
import {Image} from "@mantine/core";

const backendUrl = import.meta.env.VITE_BACKEND_URL

type Props = {
    imageInfo: ImageInfo;
}

// TODO use /thumbnail-200px here. /full is created for full image version, which is should be created.
export const ImageView: React.FC<Props> = ({imageInfo}) => {
    return <div className={"imageView"}>
        <Image src={`${backendUrl}/${imageInfo.id}`} />
    </div>
}