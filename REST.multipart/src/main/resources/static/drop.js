function dropHandler(ev) {
    ev.preventDefault();
    document.getElementById("dropsite").classList.remove("highlight");
    document.getElementById("target").files = ev.dataTransfer.files;
    document.getElementById("filenameText").innerHTML = document.getElementById("target").files[0].name;
    document.getElementById("targetName").value = document.getElementById("target").files[0].name;
}
function dragOverHandler(ev) {
    ev.preventDefault();
    document.getElementById("dropsite").classList.add("highlight");
}
function dragLeaveHandler(ev){
    document.getElementById("dropsite").classList.remove("highlight");
}
function selectFileHandler(){
    document.getElementById("filenameText").innerHTML = document.getElementById("target").files[0].name;
    document.getElementById("targetName").value = document.getElementById("target").files[0].name;
}
function buttonSelectFile(){
    document.getElementById("target").click();
}