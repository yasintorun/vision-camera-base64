import type { Frame } from 'react-native-vision-camera';


export function frameToBase64(frame: Frame): string {
  'worklet';
  // @ts-ignore
  return __frameToBase64(frame);
}