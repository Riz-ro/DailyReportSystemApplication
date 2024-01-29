/** 削除ボタンを押したときの処理 */
function clickBtnReportsDelete() {
    // 確認ダイアログを表示
    if (window.confirm(`削除して良いですか？`)) {
        // OKが押されたら処理を実行
        return true;
    } else {
        return false;
    }
}

// 削除ボタンに関数を割り当てる
document.getElementById("reportsDelete").onclick = clickBtnReportsDelete;