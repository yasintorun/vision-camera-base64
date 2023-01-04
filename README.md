# vision-camera-base64

A simple VisionCamera FrameProcessor plugin. 
Convert the frame to base64 string format.

__Note:__ This plugin is only string and not include `data:image/blabla/;base64,`

## Installation

```sh
npm install vision-camera-base64
```

Add this line to `babel.config.js`

```json
[
    'react-native-reanimated/plugin',
    {
        globals: ['__frameToBase64'],
    },
],
```

## Usage

```js
import { frameToBase64 } from 'vision-camera-base64';

// ...
const process = useFrameProcessor((frame) => {
    'worklet'
    const imageAsBase64 = frameToBase64(frame)
}, [])
```

## Contributing

See the [contributing guide](CONTRIBUTING.md) to learn how to contribute to the repository and the development workflow.

## License

MIT

---

Made with [create-react-native-library](https://github.com/callstack/react-native-builder-bob) 💜
