import { readable } from 'svelte/store'

// a readable store with initial value 0
//
// we pass it a function; the first argument of the function will let us update
// the value when it changes
export const WS = readable(0, set => {
  // this function is called once, when the first subscriber to the store arrives

  let socket = new WebSocket("ws://localhost:30000/stream");
  
  socket.onopen = function( event ) {
    console.log("Connected");
    socket.send("web-client");
  }

  socket.onmessage = function (event) {
    //var data = JSON.parse(event.data);
    let data = event.data;

    // we're using the `set` function we've been provided to update the value
    // of the store
    set(data)
  };

  // ... the rest of your socket code

  const dispose = () => {
    socket.close()
  }

  // the function we return here will be called when the last subscriber
  // unsubscribes from the store (hence there's 0 subscribers left)
  return dispose
})