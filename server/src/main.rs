use std::net::UdpSocket;

use enigo::{Enigo, Keyboard, Settings};

fn main() -> Result<(), Box<dyn std::error::Error>> {
    let mut input = Enigo::new(&Settings::default())?;
    let socket = UdpSocket::bind("0.0.0.0:8888").unwrap();
    loop {
        let mut buf = [0u8; 4096];
        let (amt, _addr) = socket.recv_from(&mut buf).unwrap();
        let msg = String::from_utf8_lossy(&buf[..amt]);
        if &msg == "exit" {
            return Ok(());
        }
        input.text(&msg)?;
    }
}
