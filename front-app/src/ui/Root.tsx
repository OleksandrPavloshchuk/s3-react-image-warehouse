import * as React from "react";
import {ActionIcon, Button, CloseIcon, Flex, Modal, Stack, TextInput} from "@mantine/core";
import {useEffect, useRef, useState} from "react";
import {addImage, type ConnectorState, getImageList} from "../services/connectToBackend.ts";
import {List} from "./List.tsx";
import type {ImageInfo} from "../dto/imageInfo.ts";

export const Root: React.FC = () => {

    const [showDialog, setShowDialog] = useState<boolean>(false)

    const [data, setData] = useState<ImageInfo[]>([])

    const [fileName, setFileName] = useState<string>("")

    const [listState, setListState] = useState<ConnectorState>("loading")
    const [addImageState, setAddImageState] = useState<ConnectorState>("ok")

    const retrieveImageList = async () => getImageList(setData, setListState)

    useEffect(() => {
        void retrieveImageList()
    }, []);

    const fileLoader = useRef<HTMLInputElement>(null)

    const doAddImage = async () => {
        const file: File | undefined = fileLoader.current?.files?.[0]
        if (file) {
            const added = await addImage(file, setAddImageState)
            if (added) {
                setShowDialog(false)
                setFileName("")
                await retrieveImageList()
            }
        }
    }

    const selectFile = () => {
        fileLoader.current?.click()
    }

    return <div className={"root"}>
        <Stack>
            <Flex>
                <Button onClick={() => setShowDialog(true)}>Add Image...</Button>
            </Flex>
            <Flex w="100%" gap="sm">
                <List data={data} state={listState}/>
            </Flex>
        </Stack>
        {showDialog &&
            <Modal
                title="Add Image"
                trapFocus={false}
                zIndex={8000}
                opened={showDialog}
                onClose={() => setShowDialog(false)}
                withinPortal={true}
            >
                <Stack>
                    <Flex>
                        <Button onClick={selectFile}>Choose image...</Button>
                        <TextInput readOnly value={fileName}/>
                        <ActionIcon
                            component="span"
                            onClick={(e) => {
                                e.stopPropagation();
                                e.preventDefault();
                                setFileName("")
                                if (fileLoader.current) {
                                    fileLoader.current.value = ""
                                }
                            }}
                            variant="light"
                            size="lg">
                            <CloseIcon/>
                        </ActionIcon>
                        <input
                            ref={fileLoader}
                            type={"file"}
                            accept={"image/*"}
                            hidden
                            onChange={(e)=> {
                                const file = e.currentTarget.files?.[0]
                                setFileName(file?.name ?? "")
                                setAddImageState("ok")
                            }}
                        />
                    </Flex>
                    <Flex>
                        {addImageState === "ok" &&
                            <Button
                                onClick={doAddImage}
                                disabled={!fileName}
                            >Add Image</Button>
                        }
                        {addImageState === "loading" &&
                            <div>Adding new image...</div>
                        }
                        {addImageState === "error" &&
                            <div>Can't add new image</div>
                        }
                    </Flex>
                </Stack>
            </Modal>
        }
    </div>;
}