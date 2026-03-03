defmodule VigiproCloud.PublicCameras.Catalog do
  @moduledoc """
  Curated list of public cameras.
  Edit this module to add/remove cameras from the public catalog.

  Stream URL prefixes:
  - "lavfi:" — FFmpeg test source, converted to HLS on our backend
  - "mjpeg:" — MJPEG over HTTP, converted to HLS on our backend (e.g. COR Rio)
  - plain URL — direct HLS stream, served as-is to the client
  """

  @cameras [
    # =====================================================
    # COR Rio de Janeiro — MJPEG cameras (converted to HLS)
    # =====================================================
    %{
      id: "cor-rj-copacabana-atlantica",
      name: "Copacabana - Av. Atlantica",
      description: "Av. Atlantica, 185 - orla de Copacabana",
      category: "tourism",
      city: "Rio de Janeiro",
      state: "RJ",
      stream_url: "mjpeg:https://aplicativo.cocr.com.br/camera/stream/1187",
      thumbnail_url: nil,
      status: :online,
      featured: true,
      sort_order: 0
    },
    %{
      id: "cor-rj-ipanema-vieira-souto",
      name: "Ipanema - Av. Vieira Souto",
      description: "Av. Vieira Souto x R. Vinicius de Moraes",
      category: "tourism",
      city: "Rio de Janeiro",
      state: "RJ",
      stream_url: "mjpeg:https://aplicativo.cocr.com.br/camera/stream/1006",
      thumbnail_url: nil,
      status: :online,
      featured: true,
      sort_order: 1
    },
    %{
      id: "cor-rj-leme-ponta",
      name: "Leme - Ponta do Leme",
      description: "Av. Atlantica x Ponta do Leme",
      category: "tourism",
      city: "Rio de Janeiro",
      state: "RJ",
      stream_url: "mjpeg:https://aplicativo.cocr.com.br/camera/stream/1219",
      thumbnail_url: nil,
      status: :online,
      featured: false,
      sort_order: 2
    },
    %{
      id: "cor-rj-botafogo-praia",
      name: "Praia de Botafogo",
      description: "Praia de Botafogo x Viaduto Santiago Dantas",
      category: "tourism",
      city: "Rio de Janeiro",
      state: "RJ",
      stream_url: "mjpeg:https://aplicativo.cocr.com.br/camera/stream/13",
      thumbnail_url: nil,
      status: :online,
      featured: true,
      sort_order: 3
    },
    %{
      id: "cor-rj-leblon-delfim",
      name: "Leblon - Av. Delfim Moreira",
      description: "Av. Delfim Moreira x R. Bartolomeu Mitre",
      category: "tourism",
      city: "Rio de Janeiro",
      state: "RJ",
      stream_url: "mjpeg:https://aplicativo.cocr.com.br/camera/stream/33",
      thumbnail_url: nil,
      status: :online,
      featured: false,
      sort_order: 4
    },
    %{
      id: "cor-rj-copacabana-posto5",
      name: "Copacabana - Posto 5",
      description: "Copacabana, proximo ao Posto 5",
      category: "tourism",
      city: "Rio de Janeiro",
      state: "RJ",
      stream_url: "mjpeg:https://aplicativo.cocr.com.br/camera/stream/1209",
      thumbnail_url: nil,
      status: :online,
      featured: false,
      sort_order: 5
    },

    # =============================================
    # COR Rio — Traffic cameras (MJPEG → HLS)
    # =============================================
    %{
      id: "cor-rj-maracana",
      name: "Maracana - Av. Maracana",
      description: "Av. Maracana, alt. Praca Varnhagen",
      category: "traffic",
      city: "Rio de Janeiro",
      state: "RJ",
      stream_url: "mjpeg:https://aplicativo.cocr.com.br/camera/stream/6834",
      thumbnail_url: nil,
      status: :online,
      featured: true,
      sort_order: 0
    },
    %{
      id: "cor-rj-aterro",
      name: "Aterro do Flamengo",
      description: "Aterro x Av. Oswaldo Cruz",
      category: "traffic",
      city: "Rio de Janeiro",
      state: "RJ",
      stream_url: "mjpeg:https://aplicativo.cocr.com.br/camera/stream/20",
      thumbnail_url: nil,
      status: :online,
      featured: true,
      sort_order: 1
    },
    %{
      id: "cor-rj-niemeyer",
      name: "Av. Niemeyer - Sao Conrado",
      description: "Av. Niemeyer proximo ao Hotel Nacional",
      category: "traffic",
      city: "Rio de Janeiro",
      state: "RJ",
      stream_url: "mjpeg:https://aplicativo.cocr.com.br/camera/stream/1004",
      thumbnail_url: nil,
      status: :online,
      featured: false,
      sort_order: 2
    },
    %{
      id: "cor-rj-copacabana-nsa",
      name: "Av. N. Sra. de Copacabana",
      description: "Av. N. Sra. de Copacabana x R. Sta. Clara",
      category: "traffic",
      city: "Rio de Janeiro",
      state: "RJ",
      stream_url: "mjpeg:https://aplicativo.cocr.com.br/camera/stream/27",
      thumbnail_url: nil,
      status: :online,
      featured: false,
      sort_order: 3
    },

    # ====================================================
    # US DOT — Direct HLS streams (no conversion needed)
    # ====================================================
    %{
      id: "dot-la-i20-shreveport",
      name: "I-20 Shreveport, Louisiana",
      description: "Interstate 20, Shreveport - Louisiana DOT",
      category: "traffic",
      city: "Shreveport",
      state: "LA",
      stream_url: "https://ITSStreamingBR2.dotd.la.gov/public/shr-cam-030.streams/playlist.m3u8",
      thumbnail_url: nil,
      status: :online,
      featured: false,
      sort_order: 10
    },
    %{
      id: "dot-wi-i41-appleton",
      name: "I-41 Appleton, Wisconsin",
      description: "Interstate 41 at County A - Wisconsin DOT",
      category: "traffic",
      city: "Appleton",
      state: "WI",
      stream_url: "https://cctv1.dot.wi.gov:443/rtplive/CCTV-44-0081/playlist.m3u8",
      thumbnail_url: nil,
      status: :online,
      featured: false,
      sort_order: 11
    },

    # ==========================================
    # Demo — FFmpeg test sources (always work)
    # ==========================================
    %{
      id: "demo-camera1",
      name: "Demo - Padrao de Teste",
      description: "Camera demo com padrao de teste colorido",
      category: "demo",
      city: nil,
      state: nil,
      stream_url: "lavfi:testsrc2=size=854x480:rate=15",
      thumbnail_url: nil,
      status: :online,
      featured: false,
      sort_order: 99
    },
    %{
      id: "demo-cam1",
      name: "Demo - Barras SMPTE",
      description: "Camera demo com barras de calibracao",
      category: "demo",
      city: nil,
      state: nil,
      stream_url: "lavfi:smptebars=size=854x480:rate=15",
      thumbnail_url: nil,
      status: :online,
      featured: false,
      sort_order: 100
    }
  ]

  @category_labels %{
    "traffic" => "Transito",
    "tourism" => "Turismo",
    "demo" => "Demo"
  }

  def all, do: @cameras

  def categories do
    @cameras
    |> Enum.group_by(& &1.category)
    |> Enum.map(fn {key, cams} ->
      %{
        key: key,
        label: Map.get(@category_labels, key, key),
        count: length(cams)
      }
    end)
    |> Enum.sort_by(& &1.key)
  end
end
