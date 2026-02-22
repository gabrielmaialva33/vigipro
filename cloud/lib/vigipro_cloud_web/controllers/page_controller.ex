defmodule VigiproCloudWeb.PageController do
  use VigiproCloudWeb, :controller

  def home(conn, _params) do
    render(conn, :home)
  end
end
