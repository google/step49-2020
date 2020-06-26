// Copyright 2020 Google LLC
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//     https://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

const loc = "/images/life/";
let currImageNum = 1;
let sliderSpeed = 5000;
let myTimer;
let paused = false;

// Changes slideshow's picture to the next picture when the user presses the next button
function next() {
  if (!paused) {
    clearInterval(myTimer);
  }
  currImageNum++;
  if (currImageNum === 17) {
    currImageNum = 1;
  }
  const newImgPath = loc + currImageNum + ".jpg";
  document.getElementById("galleryimg").src = newImgPath;
  if (!paused) {
    myTimer = setInterval(loopOverImages, sliderSpeed);
  }
}

// Changes slideshow's picture to the previous picture when the user presses the previous button
function prev() {
  if (!paused) {
    clearInterval(myTimer);
  }
  currImageNum--;
  if (currImageNum === 0) {
    currImageNum = 16;
  }
  const newImgPath = loc + currImageNum + ".jpg";
  document.getElementById("galleryimg").src = newImgPath;
  if (!paused) {
    myTimer = setInterval(loopOverImages, sliderSpeed);
  }
}

// Is called every sliderSpeed seconds, changes slideshow to display next image in list of images
function loopOverImages() {
  if (!paused) {
    currImageNum++;
    if (currImageNum === 17) {
      currImageNum = 1;
    }
    const newImgPath = loc + currImageNum + ".jpg";
    document.getElementById("galleryimg").src = newImgPath;
  }
}

// Modifies slider speed if user changes value of slider on gallery page
function sliderMoved() {
  if (!paused) {
    clearInterval(myTimer);
  }
  const sliderValue = document.getElementById("galleryslider").value;
  sliderSpeed = sliderValue * 1000;
  if (!paused) {
    myTimer = setInterval(loopOverImages, sliderSpeed);
  }
}

// Pauses or resumes slideshow when user clicks on that window
function togglePause() {
  if (paused === true) {
    const statusImg = document.getElementById("pauseplay")
    statusImg.src = "/images/play.png";
    statusImg.style.display = "block";
    window.setTimeout(function() {
      $("#pauseplay").fadeOut();
      statusImg.style.display = "none";
    }, 500);
    myTimer = setInterval(loopOverImages, sliderSpeed);
    paused = false;
  } else {
    clearInterval(myTimer);
    const statusImg = document.getElementById("pauseplay")
    statusImg.src = "/images/pause.png";
    statusImg.style.display = "block";
    window.setTimeout(function() {
      $("#pauseplay").fadeOut();
      statusImg.style.display = "none";
    }, 500);
    paused = true;
  }
}

// Initiates slideshow transition loop when gallery page loads
function startSlideshow() {
  myTimer = setInterval(loopOverImages, sliderSpeed);
}