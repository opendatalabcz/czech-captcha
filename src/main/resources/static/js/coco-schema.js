const COCO_SCHEMA = {
        "type": "object",
        "properties": {
            "info": {},
            "licenses": {},
            "categories": {
                "type": "array",
                "items": {
                    "type": "object",
                    "properties": {
                        "id": {"type": "integer", "description": "A unique internal category id"},
                        "name": {"type": "string", "pattern": "[^/]+", "description": "A unique external category name or identifier"},
                        "alias": {
                            "type": "array",
                            "items": {"type": "string", "pattern": "[^/]+"},
                            "description": "A list of alternate names that should be resolved to this category"
                        },
                        "supercategory": {
                            "anyOf": [
                                {"type": "string", "description": "A coarser category name"},
                                {"type": "null"}
                            ]
                        },
                        "parents": {
                            "type": "array",
                            "items": {"type": "string", "pattern": "[^/]+"},
                            "description": "Used for multiple inheritance"
                        },
                        "keypoints": {"description": "deprecated"},
                        "skeleton": {"description": "deprecated"}
                    },
                    "required": ["id", "name"],
                    "description": "High level information about an annotation category",
                    "title": "CATEGORY"
                }
            },
            "keypoint_categories": {
                "type": "array",
                "items": {
                    "type": "object",
                    "properties": {
                        "name": {"type": "string", "pattern": "[^/]+", "description": "The name of the keypoint category"},
                        "id": {"type": "integer"},
                        "supercategory": {
                            "anyOf": [
                                {"type": "string", "pattern": "[^/]+"},
                                {"type": "null"}
                            ]
                        },
                        "reflection_id": {
                            "anyOf": [
                                {"type": "integer"},
                                {"type": "null"}
                            ],
                            "description": "The keypoint category this should change to if the image is horizontally flipped"
                        }
                    },
                    "required": ["id", "name"],
                    "description": "High level information about an annotation category",
                    "title": "KEYPOINT_CATEGORY"
                }
            },
            "videos": {
                "type": "array",
                "items": {
                    "type": "object",
                    "properties": {
                        "id": {"type": "integer", "description": "An internal video identifier"},
                        "name": {"type": "string", "pattern": "[^/]+", "description": "A unique name for this video"},
                        "caption": {"type": "string", "description": "A video level text caption"},
                        "resolution": {
                            "anyOf": [
                                {"type": "number"},
                                {"type": "string"},
                                {"type": "null"}
                            ],
                            "description": "a unit representing the size of a pixel in video space"
                        }
                    },
                    "required": ["id", "name"],
                    "description": "High level information about a group of temporally ordered images",
                    "title": "VIDEO"
                }
            },
            "images": {
                "type": "array",
                "items": {
                    "type": "object",
                    "properties": {
                        "id": {"type": "integer", "description": "a unique internal image identifier"},
                        "file_name": {
                            "anyOf": [
                                {"type": "string", "description": "A relative or absolute path to the main image file. If this file_name is unspecified, then a name and auxiliary items or assets must be specified. Likewise this should be null if assets are used."},
                                {"type": "null"}
                            ]
                        },
                        "name": {
                            "anyOf": [
                                {"type": "string", "pattern": "[^/]+", "description": "A unique name for the image. If unspecified the file_name should be used as the default value for the name property. Required if assets / auxiliary are specified."},
                                {"type": "null"}
                            ]
                        },
                        "width": {"type": "integer", "description": "The width of the image in image space pixels"},
                        "height": {"type": "integer", "description": "The height of the image in image space pixels"},
                        "video_id": {"type": "integer", "description": "The video this image belongs to"},
                        "timestamp": {
                            "anyOf": [
                                {"type": "string", "description": "An ISO-8601 timestamp"},
                                {"type": "number", "description": "A UNIX timestamp"}
                            ]
                        },
                        "frame_index": {"type": "integer", "description": "Used to temporally order the images in a video"},
                        "channels": {
                            "anyOf": [
                                {"type": "string", "pattern": "[^/]*", "description": "A human readable channel name. Must be compatible with kwcoco.ChannelSpec", "title": "CHANNEL_SPEC"},
                                {"type": "null"}
                            ]
                        },
                        "resolution": {
                            "anyOf": [
                                {"type": "number"},
                                {"type": "string"},
                                {"type": "null"}
                            ],
                            "description": "a unit representing the size of a pixel in image space"
                        },
                        "auxiliary": {
                            "type": "array",
                            "items": {
                                "type": "object",
                                "properties": {
                                    "file_name": {"type": "string"},
                                    "channels": {"type": "string", "pattern": "[^/]*", "description": "A human readable channel name. Must be compatible with kwcoco.ChannelSpec", "title": "CHANNEL_SPEC"},
                                    "width": {"type": "integer", "description": "The width in asset-space pixels"},
                                    "height": {"type": "integer", "description": "The height in asset-space pixels"}
                                },
                                "required": ["file_name"],
                                "description": "Information about a single file belonging to an image",
                                "title": "ASSET"
                            },
                            "description": "This will be deprecated for assets in the future"
                        },
                        "assets": {
                            "type": "array",
                            "items": {
                                "type": "object",
                                "properties": {
                                    "file_name": {"type": "string"},
                                    "channels": {"type": "string", "pattern": "[^/]*", "description": "A human readable channel name. Must be compatible with kwcoco.ChannelSpec", "title": "CHANNEL_SPEC"},
                                    "width": {"type": "integer", "description": "The width in asset-space pixels"},
                                    "height": {"type": "integer", "description": "The height in asset-space pixels"}
                                },
                                "required": ["file_name"],
                                "description": "Information about a single file belonging to an image",
                                "title": "ASSET"
                            },
                            "description": "A list of assets belonging to this image, used when image channels are split across multiple files"
                        }
                    },
                    "anyOf": [
                        {
                            "required": ["id", "file_name"]
                        },
                        {
                            "required": ["id", "name", "auxiliary"]
                        },
                        {
                            "required": ["id", "name", "assets"]
                        }
                    ],
                    "description": "High level information about a image file or a collection of image files corresponding to a single point in (or small interval of) time",
                    "title": "IMAGE"
                }
            },
            "annotations": {
                "type": "array",
                "items": {
                    "type": "object",
                    "properties": {
                        "id": {"type": "integer", "description": "A unique internal id for this annotation"},
                        "image_id": {"type": "integer", "description": "The image id this annotation belongs to"},
                        "bbox": {
                            "type": "array",
                            "items": {"type": "number"},
                            "description": "[top-left x, top-left-y, width, height] in image-space pixels",
                            "title": "BBOX",
                            "minItems": 4,
                            "maxItems": 4
                        },
                        "category_id": {"type": "integer", "description": "The category id of this annotation"},
                        "track_id": {
                            "anyOf": [
                                {"type": "integer"},
                                {"type": "string"},
                                {"type": "string"}
                            ],
                            "description": "An identifier used to group annotations belonging to the same object over multiple frames in a video"
                        },
                        "segmentation": {
                            "anyOf": [
                                {
                                    "anyOf": [
                                        {
                                            "type": "object",
                                            "properties": {
                                                "exterior": {
                                                    "type": "array",
                                                    "items": {
                                                        "type": "array",
                                                        "items": {"type": "number"},
                                                        "minItems": 2,
                                                        "maxItems": 2
                                                    },
                                                    "description": "counter-clockwise xy exterior points"
                                                },
                                                "interiors": {
                                                    "type": "array",
                                                    "items": {
                                                        "type": "array",
                                                        "items": {
                                                            "type": "array",
                                                            "items": {"type": "number"},
                                                            "minItems": 2,
                                                            "maxItems": 2
                                                        },
                                                        "description": "clockwise xy hole"
                                                    }
                                                }
                                            },
                                            "title": "KWCOCO_POLYGON",
                                            "description": "A new-style polygon format that supports holes"
                                        },
                                        {
                                            "type": "array",
                                            "items": {
                                                "type": "object",
                                                "properties": {
                                                    "exterior": {
                                                        "type": "array",
                                                        "items": {
                                                            "type": "array",
                                                            "items": {"type": "number"},
                                                            "minItems": 2,
                                                            "maxItems": 2
                                                        },
                                                        "description": "counter-clockwise xy exterior points"
                                                    },
                                                    "interiors": {
                                                        "type": "array",
                                                        "items": {
                                                            "type": "array",
                                                            "items": {
                                                                "type": "array",
                                                                "items": {"type": "number"},
                                                                "minItems": 2,
                                                                "maxItems": 2
                                                            },
                                                            "description": "clockwise xy hole"
                                                        }
                                                    }
                                                },
                                                "title": "KWCOCO_POLYGON",
                                                "description": "A new-style polygon format that supports holes"
                                            }
                                        },
                                        {
                                            "type": "array",
                                            "items": {"type": "number"},
                                            "description": "an old-style polygon [x1,y1,v1,...,xk,yk,vk]",
                                            "title": "MSCOCO_POLYGON"
                                        },
                                        {
                                            "type": "array",
                                            "items": {
                                                "type": "array",
                                                "items": {"type": "number"},
                                                "description": "an old-style polygon [x1,y1,v1,...,xk,yk,vk]",
                                                "title": "MSCOCO_POLYGON"
                                            }
                                        }
                                    ]
                                },
                                {"type": "string", "description": "A run-length-encoding mask format read by pycocotools"}
                            ],
                            "description": "A polygon or mask specifying the pixels in this annotation in image-space"
                        },
                        "keypoints": {
                            "anyOf": [
                                {
                                    "type": "array",
                                    "items": {"type": "integer"},
                                    "description": "An old-style set of keypoints (x1,y1,v1,...,xk,yk,vk)",
                                    "title": "MSCOCO_KEYPOINTS"
                                },
                                {
                                    "type": "array",
                                    "items": {
                                        "type": "object",
                                        "properties": {
                                            "xy": {
                                                "type": "array",
                                                "items": {"type": "number"},
                                                "description": "<x1, y1> in pixels",
                                                "minItems": 2,
                                                "maxItems": 2
                                            },
                                            "visible": {"type": "integer", "description": "choice(0, 1, 2)"},
                                            "keypoint_category_id": {"type": "integer"},
                                            "keypoint_category": {"type": "string", "description": "only to be used as a hint"}
                                        },
                                        "title": "KWCOCO_KEYPOINT"
                                    }
                                }
                            ],
                            "description": "A set of categorized points belonging to this annotation in image space"
                        },
                        "prob": {
                            "type": "array",
                            "items": {"type": "number"},
                            "description": "This needs to be in the same order as categories. The probability order currently needs to be known a-priori, typically in *order* of the classes, but its hard to always keep that consistent. This SPEC is subject to change in the future."
                        },
                        "score": {"type": "number", "description": "Typically assigned to predicted annotations"},
                        "weight": {"type": "number", "description": "Typically given to truth annotations to indicate quality."},
                        "iscrowd": {
                            "anyOf": [
                                {"type": "integer"},
                                {"type": "boolean"}
                            ],
                            "description": "A legacy mscoco field used to indicate if an annotation contains multiple objects"
                        },
                        "caption": {"type": "string", "description": "An annotation-level text caption"}
                    },
                    "required": ["id", "image_id"],
                    "description": "Metadata about some semantic attribute of an image.",
                    "title": "ANNOTATION"
                }
            }
        },
        "required": [],
        "description": "The formal kwcoco schema",
        "title": "KWCOCO_SCHEMA"
    };

export {COCO_SCHEMA}
