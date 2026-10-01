import * as React from "react";
import {ImageView} from "./ImageView.tsx";
import type {ImageInfo} from "../dto/imageInfo.ts";
import {type ConnectorState} from "../services/connectToBackend.ts";

type Props = {
    data: ImageInfo[];
    state: ConnectorState;
}

export const List: React.FC<Props> = ({data, state}) => {

    return <div className={"list"}>
        {state==="ok" &&
            data.map((value) => <ImageView key={value.id} imageInfo={value}/>)
        }
        {state==="loading" &&
            <div>Loading...</div>
        }
        {state==="error" &&
            <div>Error reading images</div>
        }
    </div>
}